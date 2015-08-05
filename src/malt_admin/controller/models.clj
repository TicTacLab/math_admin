(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render)]
            [malt-admin.form.model :as form]
            [malt-admin.audit :refer [audit]]
            [malt-admin.offloader :as off]
            [malt-admin.storage
             [log :as slog]
             [models :as models]
             [cache :as cache]
             [configuration :as cfg]
             [in-params :as in-params]
             [cache-q :as cache-q]]
            [cheshire.core :as json]
            [malt-admin.helpers :refer [redirect-with-flash error! Packet]]
            [formative.parse :as fp]
            [org.httpkit.client :as http]
            [clojure.tools.trace :refer [trace]]
            [clojure.pprint :refer [pprint]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]
            [flatland.protobuf.core :as pb])
  (:refer-clojure :exclude [replace])
  (:import (java.nio.file Files Paths)
           [java.util UUID Date]
           [java.io File]))

(defn index [{{storage :storage} :web :as req}]
  (render "models/index" req {:models (models/get-models storage)}))

(defn upload [{:keys [problems params] :as req}]
  (render "models/upload" req {:upload-form (assoc form/upload-form
                                              :action "/models"
                                              :method "POST"
                                              :values (merge {:in_sheet_name  "IN"
                                                              :out_sheet_name "OUT"}
                                                             params)
                                              :problems problems)}))

(defn prepare-file-attrs [{{tempfile "tempfile" filename "filename" size "size" content-type "content-type"} :file :as params}]
  (if (zero? size)
    (dissoc params :file)
    (assoc params
      :file (Files/readAllBytes (Paths/get (.toURI ^File tempfile)))
      :file_name filename
      :content_type content-type)))

(defn generate-revision [params]
  (if (:file params)
    (assoc params :rev (str (UUID/randomUUID)))
    params))

(defn add-last-modified [model]
  (if (:file model)
    (assoc model :last_modified (Date.))
    model))

(defn do-upload [{params :params
                  {:keys [storage offloader]} :web
                  :as req}]
  (fp/with-fallback #(malt-admin.controller.models/upload (assoc req :problems %))
    (let [values (->> params
                      (fp/parse-params form/upload-form)
                      (prepare-file-attrs)
                      (add-last-modified)
                      (generate-revision))]

      (cond
        (models/model-exists? storage (:id values))
        (error! [:id] (str "Model with this ID already exists: " (:id values)))

        (not (contains? values :file))
        (error! [:file] "You should specify file!")

        :else
        (do
          (models/write-model! storage values)
          (audit req :upload-model (dissoc values :file))
          (off/offload-model! offloader (:id values))
          (redirect-with-flash "/models" {:success "DONE"}))))))

(defn edit [{{storage :storage} :web
             {id :id :as params} :params
             problems :problems
             :as req}]
  (let [model (models/get-model storage (Integer/valueOf ^String id))]
    (render "models/edit" req {:edit-form (assoc form/edit-form
                                            :values (if problems params model)
                                            :action (str "/models/" id)
                                            :method "PUT"
                                            :problems problems)})))

(defn replace [{{:keys [storage offloader]} :web
                params             :params
                :as                req}]
  (fp/with-fallback #(malt-admin.controller.models/edit (assoc req :problems %))
    (let [parsed-params (fp/parse-params form/edit-form params)
          values (-> parsed-params
                     (prepare-file-attrs)
                     (add-last-modified)
                     (generate-revision)
                     (select-keys [:id :file :name :file_name :content_type :in_sheet_name :out_sheet_name :rev :last_modified]))
          id (:id values)
          old-rev (models/get-rev storage id)
          new-rev (:rev values)]
      (when (:in_params_changed parsed-params)
        (in-params/delete! storage id))
      (models/replace-model! storage values)

      ;; TODO: make filler better
      #_(when (:file values)
        ;; clean and copy in-params to CACHE_Q table
        (cache-q/delete! storage id old-rev)
        (->> (in-params/get-in-params storage id)
             (cache-q/insert-in-params! storage new-rev))
        (cache/clear storage id old-rev))
      (audit req :replace-model (dissoc values :file))
      (off/offload-model! offloader id)
      (redirect-with-flash "/models" {:success (format "Model with id %d was replaced" id)}))))

(defn delete [{{id :id}           :params
               {storage :storage} :web
               :as                req}]
  (let [model-id (Integer/valueOf ^String id)
        rev (models/get-rev storage model-id)]
    (models/delete-model! storage model-id)
    (cache/clear storage model-id rev)
    (in-params/delete! storage model-id)
    (audit req :delete-model {:id model-id})
    (redirect-with-flash "/models"
                         {:success (format "Model with id %d was deleted"
                                           model-id)})))

(defn download [{{id :id} :params
                 {storage :storage} :web
                 :as req}]
  (let [file (models/get-model-file storage (Integer/valueOf ^String id))]
    (audit req :download-model {:id id})
    {:body    (:file file)
     :headers {"Content-Type"        (:content_type file)
               "Content-Disposition" (str "attachment; filename=" (:file_name file))}}))

(defn- malt-params->form-fileds [malt-params]
  (let [fields (some->> malt-params
                        (sort-by :id)
                        (map (juxt (comp keyword str :id)
                                   #(format "%s. %s" (:id %) (:code %))
                                   (constantly :text)))
                        (map (partial zipmap [:name :label :type])))
        submit (vector {:name :submit :type :submit :value "Calculate"})]
    (concat submit fields submit)))

(defn- malt-params->form-values [malt-params]
  (some->> malt-params
           (map (juxt (comp str :id) :value))
           (into {})))

(defn- malt-params->form [malt-params]
  (hash-map :fields (malt-params->form-fileds malt-params)
            :validations (->> malt-params
                              (map (comp keyword str :id))
                              (vector :required)
                              vector)))

(defn make-model-sid [id rev ssid]
  (str ssid \- id \- rev))

(defn- get-malt-params [node port model-id rev ssid]
  (let [url (format "http://%s:%s/models/%s/%s/in-params"
                    node
                    port
                    model-id
                    (make-model-sid model-id rev ssid))
        {:keys [status error body]} @(http/get url {:as :text})
        response (json/parse-string body true)]
    (cond
      error (log/error error "Error when get-malt-params")
      (not= 200 status) (log/error "Server error response in get-malt-params: " response)
      :else (:data response))))

(defn remove-invalid-outcomes [outcomes]
  (let [has-valid-coef? (comp number? :coef)]
    (filter has-valid-coef? outcomes)))

(defn wrap-error
  ([msg]
   (log/error msg)
   [:error msg])
  ([e msg]
    (log/error e msg)
   [:error (str msg " " (.getLocalizedMessage e))]))

(defn- calculate [node port ssid id rev params]
  (let [malt-session-id (make-model-sid id rev ssid)
        url (format "http://%s:%s/models/%s/%s/profile"
                    node port id malt-session-id)
        malt-params {:model_id id
                     :event_id malt-session-id
                     :params   (map (fn [[id value]] {:id id :value value}) params)}
        {:keys [body error status]} @(http/post url {:body    (json/generate-string malt-params)
                                                     :timeout 60000
                                                     :as      :text})
        json-response (json/parse-string body true)]
    (cond
      error (wrap-error error "Error while calculate")
      (not= status 200) (wrap-error (str "Unexpected result while calculate: " json-response))
      :else [:ok (:data json-response)])))

(defn split [pred coll]
  [(filter pred coll) (remove pred coll)])


(defn make-weightened-comparator [calc-result & [pairs-fn]]
  (let [priority-map (->> calc-result
                          (map pairs-fn)
                          (into {}))
        priority-map (assoc priority-map "" Long/MAX_VALUE) ;; special case for mgp_code, when there is no value for it
        ]
    (comparator
      (fn [key1 key2]
        (< (or (get priority-map key1) 0)
           (or (get priority-map key2) 0))))))

(defn make-market-comparator [outcomes]
  (let [priority-map (->> outcomes
                          (map (juxt :mn_code :mn_weight))
                          (into {}))]
    (fn [key1 key2]
      (let [priority1 (get priority-map (first key1) (Integer/MAX_VALUE))
            priority2 (get priority-map (first key2) (Integer/MAX_VALUE))
            pkey1 (into [priority1] key1)
            pkey2 (into [priority2] key2)]
        (compare pkey1 pkey2)))))


(defn stringify-params [calc-result]
  (->> calc-result
       (map #(update-in % [:param] str))
       (map #(update-in % [:param2] str))))

(defn format-calc-result [calc-result]
  (let [calc-result (into {} calc-result)
        mgp-comparator (-> calc-result
                           :data
                           (make-weightened-comparator
                             (juxt :mgp_code :mgp_weight)))
        mn-comparator (-> calc-result
                          :data
                          (make-market-comparator))]
    (update-in calc-result [:data]
               (fn [data]
                 (->> data
                      (stringify-params)
                      (group-by :mgp_code)
                      (map (fn [[mgp_code outcomes]]
                             (vector mgp_code (->> outcomes
                                                   (group-by (juxt :mn_code :param :param2))
                                                   ;; sort by mn-weight
                                                   (sort-by first mn-comparator)
                                                   ;; split buses
                                                   (split (fn [[_market outcomes]]
                                                            (<= (count outcomes) 3)))
                                                   (apply concat)
                                                   ;; partition bus for 6 columns
                                                   (map (fn [[market outcomes]]
                                                          [market (partition-all 6 outcomes)]))))))
                      (into (sorted-map-by mgp-comparator)))))))

(defn render-profile-page [req model-id rev & {:keys [problems flash in-params out-params log-session-id]}]
  (let [{malt-host :profiling-malt-host
         malt-port :profiling-malt-port} (-> req :web :storage cfg/read-settings)
        malt-params (get-malt-params malt-host malt-port model-id rev (:session-id req))
        values (or in-params
                   (malt-params->form-values malt-params))
        {model-file :file_name model-name :name} (-> req
                                                     :web
                                                     :storage
                                                     (models/get-model model-id))
        total-timer (->> out-params
                         :data
                         (map :timer)
                         (reduce + 0))]
    (render "models/profile"
            (assoc req :flash flash)
            {:model-id model-id
             :model-file (->> model-file
                              (re-matches #"^(.*)\..*$")
                              second)
             :model-name model-name
             :log-session-id log-session-id
             :json-out-params (json/generate-string out-params)
             :calc-result (-> out-params format-calc-result)
             :calc-result-total-timer total-timer
             :profile-form (merge (malt-params->form malt-params)
                                  {:action (str "/models/" model-id "/" rev "/profile")
                                   :method "POST"
                                   :values values
                                   :submit-label "Calculate"
                                   :problems problems})})))

(defn profile [{params :params :as req}]
  (render-profile-page req (Integer/valueOf ^String (:id params)) (:rev params)))

(defn profile-execute [{session-id :session-id
                        params :params :as req}]
  (let [id (Integer/valueOf ^String (:id params))
        rev (:rev params)
        {malt-host :profiling-malt-host
         malt-port :profiling-malt-port} (-> req :web :storage cfg/read-settings)
        form (some->> (get-malt-params malt-host malt-port id rev session-id)
                      malt-params->form)
        in-params (dissoc params :id :submit :rev)
        render (partial render-profile-page req id rev :in-params in-params)]

    (fp/with-fallback #(render-profile-page req id
                                            :problems %
                                            :in-params in-params)
      (fp/parse-params form in-params)
      (let [[type result] (calculate malt-host
                                     malt-port
                                     session-id
                                     id
                                     rev
                                     in-params)]
        (condp = type
          :ok (render :out-params {:data (remove-invalid-outcomes result)})
          :error (render :flash {:error result}))))))

(defn read-log [{{storage :storage} :web
                 {:keys [id ssid]} :params :as req}]
  (let [model-id (Integer/valueOf ^String id)]
    (if-let [result (slog/read-log storage model-id ssid)]
      (render-profile-page req model-id
                           :in-params (-> result
                                          :in_params
                                          (json/parse-string true)
                                          malt-params->form-values)
                           :out-params (:out_params result)
                           :log-session-id ssid
                           :flash {:success "Loaded from log."})
      (render-profile-page req model-id
                           :log-session-id ssid
                           :flash {:error "No such log entry."}))))
