(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [malt-admin.form.model :as form]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [formative.core :as f]
            [malt-admin.storage.models :as storage]))

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

(defn ^:private file->bytes [file]
  (.getBytes (slurp (:tempfile file))))

(defn ^:private prepare-file-attrs [{{:keys [tempfile filename]} :file :as params}]
  (assoc params
    :file (.getBytes (slurp tempfile))
    :file_name filename))

(defn do-upload [{params :params
                  {storage :storage} :web
                  :as req}]
  (fp/with-fallback #(malt-admin.controller.models/upload (assoc req :problems %))
    (let [values (prepare-file-attrs (fp/parse-params form/upload-form params))]
      (storage/write-model! storage values)
      (res/redirect "/models"))))