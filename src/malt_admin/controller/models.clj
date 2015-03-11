(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as cfg]
            [malt-admin.form.model :as form]
            [malt-admin.audit :refer [audit]]
            [cheshire.core :as json]
            [malt-admin.helpers :refer [csv-to-list redirect-with-flash error!]]
            [formative.parse :as fp]
            [org.httpkit.client :as http]
            [clojure.tools.trace :refer [trace]]
            [clojure.pprint :refer [pprint]]
            [malt-admin.storage.models :as storage]
            [malt-admin.storage.cache :as cache]
            [clojure.tools.logging :as log])
  (:refer-clojure :exclude [replace])
  (:import (java.nio.file Files Paths)))



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
    (if (storage/model-exists? storage (Integer. (:id params)))
      (error! [:id] (str "Model with this ID already exists: " (:id params)))
      (let [values (->> params
                        (fp/parse-params form/upload-form)
                        (prepare-file-attrs))]
        (storage/write-model! storage values)
        (audit req :upload-model (dissoc values :file))
        (redirect-with-flash "/models" {:success "DONE"})))))

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
                     (select-keys [:id :file :file_name :content_type :in_sheet_name :out_sheet_name]))]
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
  (some->> malt-params
           (sort-by :id)
           (map (fn [{:keys [id name type code]}]
                  (let [f-label (format "%s. %s (%s/%s)" id name type code)
                        f-name (-> id str keyword)]
                    {:name f-name :label f-label :type :text})))))

(defn- malt-params->form-values [malt-params]
  (some->> malt-params
           (map (fn [{:keys [id value]}]
                  (vector (-> id str keyword)
                          value)))
           (into {})))

(defn- malt-params->form-validations [malt-params]
  (some->> malt-params
           (map :id)
           (map str)
           (map keyword)))

(defn- malt-params->form
  ([malt-params]
     (malt-params->form malt-params []))
  ([malt-params values]
     {:fields (malt-params->form-fileds malt-params)
      :values values
      :validations [[:required (malt-params->form-validations malt-params)]]}))

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

(defn- calculate-in-params [node port ssid id params]
  (try
    (let [url (format "http://%s:%s/model/calc"
                      node
                      port)
          malt-params {:id id
                       :ssid (make-model-sid id ssid)
                       :params (map (fn [[id value]] {:id id :value value}) params)}
          {:keys [body error status]} @(http/post url {:body (json/generate-string malt-params)
                                                       :headers {"Content-type" "text/plain"}
                                                       :timeout 60000})]
      (if error (throw error))
      (if-not (= status 200)
        (throw (RuntimeException. (format "%s %s" status body))))
      {:result body})
    (catch Exception e
      (log/error e "While malt calculation")
      {:error (format "Error: %s" (.getLocalizedMessage e))})))

(defn profile [{params :params
                problems :problems
                calc-result :calc-result
                session-id :session-id
                {storage :storage} :web :as req}]
  (let [id (:id params)
        {malt-host :profiling-malt-host
         malt-port :profiling-malt-port} (cfg/read-settings storage)
        malt-params (get-malt-params malt-host malt-port id session-id)
        values (if (contains? params :submit)
                 params
                 (malt-params->form-values malt-params))
        form (malt-params->form malt-params values)]
    (render "models/profile" req {:calc-result (json/parse-string calc-result)
                                  :profile-form (merge form
                                                       {:action (str "/models/" id "/profile")
                                                        :method "POST"
                                                        :submit-label "Calculate"
                                                        :problems problems})})))

(defn profile-execute [{{storage :storage} :web
                        session-id :session-id
                        params :params :as req}]
  (let [id (:id params)
        {malt-host :profiling-malt-host
         malt-port :profiling-malt-port} (cfg/read-settings storage)
        form (some->> (get-malt-params malt-host malt-port id session-id)
                      malt-params->form)]
    (fp/with-fallback #(profile (assoc req :problems %))
      (fp/parse-params  form params)
      (let [result (calculate-in-params malt-host
                                        malt-port
                                        session-id
                                        id
                                        (dissoc params :submit :id))]
        (if (contains? result :result)
          (profile (assoc req
                     :calc-result (:result result)
                     :flash {:success "DONE"}))
          (profile (assoc req :flash result)))))))
