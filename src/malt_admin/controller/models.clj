(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as cfg]
            [malt-admin.form.model :as form]
            [malt-admin.audit :refer [audit]]
            [malt-admin.storage.log :as slog]
            [cheshire.core :as json]
            [malt-admin.helpers :refer [csv-to-list redirect-with-flash error! Packet]]
            [formative.parse :as fp]
            [org.httpkit.client :as http]
            [clojure.tools.trace :refer [trace]]
            [clojure.pprint :refer [pprint]]
            [malt-admin.storage.models :as storage]
            [malt-admin.storage.cache :as cache]
            [clojure.string :as string]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]
            [flatland.protobuf.core :as pb])
  (:refer-clojure :exclude [replace])
  (:import (java.nio.file Files Paths)
           (flatland.protobuf PersistentProtocolBufferMap$Def)))

(defn index [{{storage :storage} :web :as req}]
  (render "models/index" req {:models (storage/get-models storage)}))

(defn upload [{:keys [problems params] :as req}]
  (render "models/upload" req {:upload-form (assoc form/upload-form
                                              :action "/models"
                                              :method "POST"
                                              :values (merge {:in_sheet_name  "IN"
                                                              :out_sheet_name "OUT"}
                                                             params)
                                              :problems problems)}))

(defn ^:private prepare-file-attrs [{{tempfile "tempfile" filename "filename" size "size" content-type "content-type"} :file :as params}]
  (if (zero? size)
    (dissoc params :file)
    (assoc params
      :file (Files/readAllBytes (Paths/get (.toURI tempfile)))
      :file_name filename
      :content_type content-type)))

(defn do-upload [{params :params
                  {storage :storage} :web
                  :as req}]
  (fp/with-fallback #(malt-admin.controller.models/upload (assoc req :problems %))
    (let [values (->> params
                      (fp/parse-params form/upload-form)
                      (prepare-file-attrs))]

      (cond
        (storage/model-exists? storage (:id values))
        (error! [:id] (str "Model with this ID already exists: " (:id values)))

        (not (contains? values :file))
        (error! [:file] "You should specify file!")

        :else
        (do
          (storage/write-model! storage values)
          (audit req :upload-model (dissoc values :file))
          (redirect-with-flash "/models" {:success "DONE"}))))))

(defn edit [{{storage :storage} :web
             {id :id :as params} :params
             problems :problems
             :as req}]
  (let [model (storage/get-model storage (Integer. id))]
    (render "models/edit" req {:edit-form (assoc form/edit-form
                                            :values (if problems params model)
                                            :action (str "/models/" id)
                                            :method "PUT"
                                            :problems problems)})))

(defn replace [{{storage :storage} :web
                params             :params
                :as                req}]
  (fp/with-fallback #(malt-admin.controller.models/edit (assoc req :problems %))
    (let [values (-> params
                     (#(fp/parse-params form/edit-form %))
                     (prepare-file-attrs)
                     (select-keys [:id :file :name :file_name :content_type :in_sheet_name :out_sheet_name]))]
      (storage/replace-model! storage values)
      (cache/clear storage (:id values))
      (audit req :replace-model (dissoc values :file))
      (redirect-with-flash "/models" {:success "DONE"}))))

(defn delete [{{id :id} :params
               {storage :storage} :web
               :as req}]
  (storage/delete-model! storage (Integer. id))
  (cache/clear storage (Integer. id))
  (audit req :delete-model {:id id})
  (redirect-with-flash "/models" {:success "DONE"}))

(defn download [{{id :id} :params
                 {storage :storage} :web
                 :as req}]
  (let [file (storage/get-model-file storage (Integer. id))]
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

(defn make-model-sid [id ssid]
  (str ssid \- id))

(defn- get-malt-params [node port model-id ssid]
  (let [url (format "http://%s:%s/model/in-params"
                    node
                    port)]
    (some-> url
            (http/get {:query-params {:ssid (make-model-sid model-id ssid) :id model-id} :as :text})
            deref
            :body
            (json/parse-string true))))

(defn parse-calc-result! [body]
  (let [packet (pb/protobuf-load Packet body)]
    (if (= (:type packet) :error)
      (throw (RuntimeException. (if (= (:error_type packet) :inprogress)
                                  "Calculation already in progress"
                                  (:error packet)))))
    {:result packet}))

(defn- calculate [node port ssid id params]
  (try
    (let [url (format "http://%s:%s/model/calc"
                      node
                      port)
          malt-params {:id id
                       :ssid (make-model-sid id ssid)
                       :params (map (fn [[id value]] {:id id :value value}) params)}
          {:keys [body error status]} @(http/post url {:body    (json/generate-string malt-params)
                                                       :headers {"Content-type" "text/plain"}
                                                       :timeout 60000
                                                       :as      :byte-array})]
      (when error (throw error))
      (when-not (= status 200)
        (throw (RuntimeException. (format "Bad Status Code: %d" status))))
      (parse-calc-result! body))
    (catch Exception e
      (log/error e "While malt calculation")
      {:error (format "Error: %s" (.getLocalizedMessage e))})))

(defn split [pred coll]
  [(filter pred coll) (remove pred coll)])


(defn- make-weightened-comparator [calc-result]
  (let [priority-map (->> calc-result
                          (map (juxt :mgp_code :mgp_weight))
                          (into {}))
        priority-map (assoc priority-map "" Long/MAX_VALUE)]
    (comparator
      (fn [key1 key2]
        (< (get priority-map key1)
           (get priority-map key2))))))



(defn format-calc-result [calc-result]
  (let [calc-result (into {} calc-result)] ;; from protobuf map-like
    (trace (update-in calc-result [:data]
                      (fn [data]
                        (->> data
                             (group-by :mgp_code)
                             (map (fn [[mgp_code outcomes]]
                                    (vector mgp_code (->> outcomes
                                                          (group-by (juxt :mn_code :param))
                                                          (split (fn [[_market outcomes]]
                                                                   (<= (count outcomes) 3)))
                                                          (mapcat (fn [part]
                                                                    (into (sorted-map-by #(compare (apply str %1)
                                                                                                   (apply str %2)))
                                                                          part)))
                                                          (map (fn [[market outcomes]]
                                                                 [market (partition-all 6 outcomes)]))))))
                             (into (sorted-map-by (make-weightened-comparator data)))))))))

(defn render-profile-page [req model-id & {:keys [problems flash in-params out-params log-session-id]}]
  (let [model-id (Integer/valueOf model-id)
        {malt-host :profiling-malt-host
         malt-port :profiling-malt-port} (-> req :web :storage cfg/read-settings)
         malt-params (get-malt-params malt-host malt-port model-id (:session-id req))
         values (or in-params
                    (malt-params->form-values malt-params))
        {model-file :file_name model-name :name} (-> req
                                                     :web
                                                     :storage
                                                     (storage/get-model model-id))]
    (render "models/profile"
            (assoc req :flash flash)
            {:model-id model-id
             :model-file (->> model-file
                              (re-matches #"^(.*)\..*$")
                              second)
             :model-name model-name
             :log-session-id log-session-id
             :calc-result (-> out-params format-calc-result)
             :profile-form (merge (malt-params->form malt-params)
                                  {:action (str "/models/" model-id "/profile")
                                   :method "POST"
                                   :values values
                                   :submit-label "Calculate"
                                   :problems problems})})))

(defn profile [{params :params
                problems :problems
                calc-result :calc-result
                session-id :session-id
                {storage :storage} :web :as req}]
  (let [id (:id params)]
    (render-profile-page req (:id params))))

(defn profile-execute [{session-id :session-id
                        params :params :as req}]
  (let [id (:id params)
        {malt-host :profiling-malt-host
         malt-port :profiling-malt-port} (-> req :web :storage cfg/read-settings)
        form (some->> (get-malt-params malt-host malt-port id session-id)
                      malt-params->form)
        in-params (dissoc params :id :submit)]

    (fp/with-fallback #(render-profile-page req id
                                            :problems %
                                            :in-params in-params)
      (fp/parse-params form in-params)
      (let [result (calculate malt-host
                              malt-port
                              session-id
                              id
                              in-params)]

        (if (contains? result :result)
          (render-profile-page req id
                               :in-params in-params
                               :out-params (:result result))
          (render-profile-page req id
                               :in-params in-params
                               :flash result))))))

(defn read-log [{{storage :storage} :web
                 {:keys [id ssid]} :params :as req}]
  (if-let [result (slog/read-log storage (Integer/valueOf id) ssid)]
    (render-profile-page req id
                         :in-params (-> result
                                        :in_params
                                        (json/parse-string true)
                                        malt-params->form-values)
                         :out-params (:out_params result)
                         :log-session-id ssid
                         :flash {:success "Loaded from log."})
    (render-profile-page req id
                         :log-session-id ssid
                         :flash {:error "No such log entry."})))
