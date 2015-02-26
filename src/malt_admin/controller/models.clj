(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [malt-admin.form.model :as form]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [formative.core :as f]
            [malt-admin.storage.models :as storage])
  (:refer-clojure :exclude [replace]))

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

(defn ^:private prepare-file-attrs [{{:keys [tempfile filename size]} :file :as params}]
  (if (zero? size)
    (dissoc params :file)
    (assoc params
      :file (.getBytes (slurp tempfile))
      :file_name filename)))

(defn do-upload [{params :params
                  {storage :storage} :web
                  :as req}]
  (fp/with-fallback #(malt-admin.controller.models/upload (assoc req :problems %))
    (let [values (->> params
                      (prepare-file-attrs)
                      (fp/parse-params form/upload-form)
                      )]
      (storage/write-model! storage values)
      (res/redirect "/models"))))

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
                     (prepare-file-attrs)
                     (#(fp/parse-params form/edit-form %))
                     (select-keys [:id :file :file_name :in_sheet_name :out_sheet_name]))]
      (storage/replace-model! storage values)
      (res/redirect "/models"))))