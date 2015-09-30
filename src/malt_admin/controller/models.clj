(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render u)]
            [malt-admin.form.model :as form]
            [malt-admin.audit :as audit]
            [malt-admin.storage
             [log :as slog]
             [models :as models]
             [cache :as cache]
             [in-params :as in-params]
             [cache-q :as cache-q]]
            [cheshire.core :as json]
            [malt-admin.helpers :refer [redirect-with-flash error!]]
            [formative.parse :as fp]
            [org.httpkit.client :as http]
            [clojure.tools.trace :refer [trace]]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]
            [malt-admin.offloader :as off]
            [schema.core :as s]
            [malcolmx.core :as mx]
            [clojure.string :as str]
            [ring.util.response :as res])
  (:refer-clojure :exclude [replace])
  (:import (java.nio.file Files Paths)
           [java.util UUID Date]
           [java.io File ByteArrayInputStream]))

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
          (audit/info req :upload-model (dissoc values :file))
          (off/offload-model! offloader (:id values))
          (redirect-with-flash "/models" {:success "DONE"}))))))

(defn edit [{{storage :storage} :web
             {id :id :as params} :params
             problems :problems
             :as req}]
  (let [model (models/get-model storage (Integer/valueOf ^String id))]
    (render "models/edit" req {:edit-form (assoc form/edit-form
                                            :values (if problems params model)
                                            :action (str "/models/" (u id))
                                            :method "PUT"
                                            :problems problems)})))

(defn replace-model [{{:keys [storage offloader]} :web
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
      (models/replace-model! storage values)

      #_(when (:file values)
        ;; clean and copy in-params to CACHE_Q table
        (cache-q/delete! storage id old-rev)
        (->> (in-params/get-in-params storage id)
             (cache-q/insert-in-params! storage new-rev))
        (cache/clear storage id old-rev))
      (audit/info req :replace-model (dissoc values :file))
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
    (audit/info req :delete-model {:id model-id})
    (redirect-with-flash "/models"
                         {:success (format "Model with id %d was deleted"
                                           model-id)})))

(defn download [{{id :id} :params
                 {storage :storage} :web
                 :as req}]
  (let [file (models/get-model-file storage (Integer/valueOf ^String id))]
    (audit/info req :download-model {:id id})
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
  (->> malt-params
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

(defn- get-malt-params [api-addr model-id rev ssid]
  (let [url (format "http://%s/models/%s/%s/in-params"
                    api-addr
                    model-id
                    (make-model-sid model-id rev ssid))
        {:keys [status error body]} @(http/get url {:query-params {:plain "true"}
                                                    :as :text})
        response (json/parse-string body true)]
    (cond
      error (log/error error "Error when get-malt-params")
      (not= 200 status) (log/error "Server error response in get-malt-params: " response)
      :else (:data response))))

(defn- get-model-out-values-header [api-addr model-id rev ssid]
  (let [url (format "http://%s/models/%s/%s/out-values-header"
                    api-addr
                    model-id
                    (make-model-sid model-id rev ssid))
        {:keys [status error body]} @(http/get url {:as :text})
        response (json/parse-string body true)]
    (cond
      error (log/error error "Error when get-model-out-values-header")
      (not= 200 status) (log/error "Server error response in get-model-out-values-header: " response)
      :else (map keyword (:data response)))))

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

(defn- calculate [api-addr ssid id rev params]
  (let [malt-session-id (make-model-sid id rev ssid)
        url (format "http://%s/models/%s/%s/profile"
                    api-addr id malt-session-id)
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

(defn format-timer-value [out-value]
  (update-in out-value [:timer] #(format "%.2f" %)))

(defn render-profile-page [req model-id rev & {:keys [problems flash in-params out-params log-session-id]}]
  (let [{api-addr :api-addr} (:web req)
        malt-params (get-malt-params api-addr model-id rev (:session-id req))
        values (or in-params
                   (malt-params->form-values malt-params))
        {model-file :file_name model-name :name} (-> req
                                                     :web
                                                     :storage
                                                     (models/get-model model-id))
        total-timer (->> out-params
                         :data
                         (map :timer)
                         (reduce + 0))
        out-values (:data out-params)
        out-header (when out-values
                     (get-model-out-values-header api-addr model-id rev (:session-id req)))]
    (render "models/profile"
            (assoc req :flash flash)
            {:model-file   (->> model-file
                                (re-matches #"^(.*)\..*$")
                                second)
             :model-name   model-name
             :out-values   (some->> out-values
                                    (map format-timer-value)
                                    (map (apply juxt (concat out-header [:timer]))))
             :out-header   (some->> out-header (map name))
             :total-timer  total-timer
             :profile-form (merge (malt-params->form malt-params)
                                  {:action       (str "/models/" (u model-id) "/" (u rev) "/profile")
                                   :method       "POST"
                                   :values       values
                                   :submit-label "Calculate"
                                   :problems     problems})})))

(defn profile [{params :params :as req}]
  (render-profile-page req (Integer/valueOf ^String (:id params)) (:rev params)))

(defn profile-execute [{session-id :session-id
                        params :params :as req}]
  (let [id (Integer/valueOf ^String (:id params))
        rev (:rev params)
        {api-addr :api-addr} (:web req)
        form (some->> (get-malt-params api-addr id rev session-id)
                      malt-params->form)
        in-params (dissoc params :id :submit :rev :csrf)
        render (partial render-profile-page req id rev :in-params in-params)]

    (fp/with-fallback #(render-profile-page req id
                                            :problems %
                                            :in-params in-params)
      (fp/parse-params form in-params)
      (let [[type result] (calculate api-addr
                                     session-id
                                     id
                                     rev
                                     in-params)]
        (condp = type
          :ok (render :out-params {:data (remove-invalid-outcomes result)})
          :error (render :flash {:error result}))))))

(defn delete-session [{ssid :session-id
                       {:keys [id rev]} :params
                       :as req}]
  (let [{api-addr :api-addr} (:web req)]
    @(http/delete (format "http://%s/models/%s/%s"
                          api-addr id (make-model-sid id rev ssid))
                  {:body    ""
                   :timeout 1000
                   :as      :text})
    {:status 200}))

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
(s/defrecord Draft
  [file :- bytes
   size :- s/Int
   file-name :- s/Str
   content-type :- s/Str])

(s/defn upload-draft*
  [web draft :- Draft session-id]
  (prn (:file-name draft))
  (cond
    (> (:size draft) (:max-file-size web))
    {:errors ["File is too big"]}

    (not (mx/excel-file? (ByteArrayInputStream. (:file draft))
                         (last (str/split (:file-name draft) #"\."))))
    {:errors ["File should be .xls or .xlsx type"]}

    :else
    (do
      (models/write-draft-model! (:storage web) draft session-id)
      {:ok ""})))

(defn ok? [result]
  (:ok result))

(defn upload-draft [{{file :file} :params
                     ssid :session-id
                     web :web}]
  (let [draft (Draft. (Files/readAllBytes (Paths/get (.toURI ^File (:tempfile file))))
                      (:size file)
                      (:filename file)
                      (:content-type file))
        res (upload-draft* web draft ssid)]
    (res/content-type
      (if (ok? res)
        {:status 200 :body "{\"ok\":\"ok\"}"}
        {:status 400 :body (json/generate-string (select-keys res [:errors]))})
      "application/json")))