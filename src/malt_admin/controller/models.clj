(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [malt-admin.form.model :as form]
            [malt-admin.helpers :refer [csv-to-list]]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [formative.core :as f]
            [org.httpkit.client :as http]
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

(defn make-notify-malts-result-flash [malt-results]
  (let [failed-hosts (->> malt-results
                          (remove (comp #(= 200 %) second))
                          clojure.tools.trace/trace
                          (map (fn [[host code]] (format "%s(%d)" host code))))]
    (if (empty? failed-hosts)
      {:success "Malts notified!"}
      {:error (str "Failed to nofity malts: " (clojure.string/join ", " failed-hosts))})))

(defn redirect-with-flash [url flash]
  (assoc (res/redirect-after-post url) :flash flash))

(defn index [{{storage :storage} :web
              flash :flash :as req}]
  (render "models/index" req {:models (storage/get-models storage)
                              :flash flash}))

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
  (->> (notify-malts storage id)
       make-malts-notify-result-flash
       (redirect-with-flash "/models")))

(defn download [{{id :id} :params
                 {storage :storage} :web
                 :as req}]
  (let [file (storage/get-model-file storage (Integer. id))]
    {:body    (:file file)
     :headers {"Content-Type"        (:content_type file)
               "Content-Disposition" (str "attachment; filename=" (:file_name file))}}))
