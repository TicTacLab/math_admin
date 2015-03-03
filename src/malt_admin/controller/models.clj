(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [malt-admin.form.model :as form]
            [malt-admin.helpers :refer [csv-to-list redirect-with-flash]]
            [cheshire.core :as json]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [formative.core :as f]
            [org.httpkit.client :as http]
            [clojure.tools.trace :refer [trace]]
            [clojure.pprint :refer [pprint]]
            [malt-admin.storage.models :as storage]
            [clojure.tools.logging :as log])
  (:refer-clojure :exclude [replace])
  (:import (java.nio.file Files Paths)))


(defn notify-malt [node port url model-id]
  (try (let [{:keys [status error]} @(http/post (str "http://" node ":" port url)
                                                {:timeout 3000
                                                 :form-params {:id model-id}})]
         (if error (throw error))
         (if-not (= status 200)
           (log/errorf "Malt %s returned code %d while notifying" node status))
         status)
       (catch Exception e
         (log/errorf e "Error while notifying malt %s" node)
         400)))

(defn notify-malts [storage model-id]
  (let [{:keys [malt-nodes rest-port malt-reload-model-url] :as config} (st/read-config storage)
        malt-nodes (csv-to-list malt-nodes)]
    (zipmap malt-nodes
            (map #(notify-malt % rest-port malt-reload-model-url model-id) malt-nodes))))

(defn make-malts-notify-result-flash [malt-results]
  (let [failed-hosts (->> malt-results
                          (remove (comp #(= 200 %) second))
                          (map (fn [[host code]] (format "%s(%d)" host code))))]
    (if (empty? failed-hosts)
      {:success "Malts notified!"}
      {:error (str "Failed to nofity malts: " (clojure.string/join ", " failed-hosts))})))

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
      (storage/write-model! storage values)
      (->> (:id values)
           (notify-malts storage)
           make-malts-notify-result-flash
           (redirect-with-flash "/models")))))

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
      (->> (:id values)
           (notify-malts storage)
           make-malts-notify-result-flash
           (redirect-with-flash "/models")))))

(defn delete [{{id :id} :params
               {storage :storage} :web
               :as req}]
  (storage/delete-model! storage (Integer. id))
  (->> id
       (notify-malts storage)
       make-malts-notify-result-flash
       (redirect-with-flash "/models")))

(defn download [{{id :id} :params
                 {storage :storage} :web
                 :as req}]
  (let [file (storage/get-model-file storage (Integer. id))]
    {:body    (:file file)
     :headers {"Content-Type"        (:content_type file)
               "Content-Disposition" (str "attachment; filename=" (:file_name file))}}))




(defn- malt-params->form-fileds [malt-params]
  (some->> malt-params
           (map (fn [{:keys [id name type code]}]
                  (let [f-label (format "%s (%s/%s)" name type code)
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

(defn- get-malt-params [node port model-id]
  (let [url (format "http://%s:%s/model/%s/in-params"
                    node
                    port
                    model-id)]
    (some-> url
            (http/get {:as :text})
            deref
            :body
            (json/parse-string true))))

(defn- calculate-in-params [node port ssid id params]
  (try
    (let [url (format "http://%s:%s/model/calc"
                      node
                      port)
          malt-params {:id id
                       :ssid ssid
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
                {storage :storage} :web :as req}]
  (let [id (:id params)
        {:keys [calculation-malt-node calculation-malt-port]} (st/read-config storage)
        malt-params (get-malt-params calculation-malt-node calculation-malt-port id)
        values (if (contains? params :submit)
                 params
                 (malt-params->values malt-params))
        form (malt-params->form malt-params values)]
    (render "models/profile" req {:calc-result (json/parse-string calc-result)
                                  :profile-form (merge form
                                                       {:action (str "/models/" id "/profile")
                                                        :method "POST"
                                                        :submit-label "Calculate"
                                                        :problems problems})})))

(defn profile-execute [{{storage :storage} :web
                        params :params :as req}]
  (let [id (:id params)
        {:keys [calculation-malt-node calculation-malt-port]} (st/read-config storage)
        ssid "BADA55"
        form (some->> id
                      (get-malt-params calculation-malt-node calculation-malt-port)
                      malt-params->form)]
    (fp/with-fallback #(profile (assoc req :problems %))
      (fp/parse-params  form params)
      (let [result (calculate-in-params calculation-malt-node
                                        calculation-malt-port
                                        ssid
                                        id
                                        (dissoc params :submit :id))]
        (if (contains? result :result)
          (profile (assoc req
                     :calc-result (:result result)
                     :flash {:success "DONE"}))
          (profile (assoc req :flash result)))))))
