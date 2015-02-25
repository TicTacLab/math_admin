(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [malt-admin.form.model :as form]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [formative.core :as f]))

(defn index [req]
  (render "models/index" req {}))

(defn upload [{:keys [problems params] :as req}]
  (render "models/upload" req {:upload-form (assoc form/upload-form
                                              :action "/models"
                                              :method "POST"
                                              :values (merge {:in-sheet-name  "IN"
                                                              :out-sheet-name "OUT"}
                                                             params)
                                              :problems problems)}))

(defn do-upload [{params :params :as req}]
  (fp/with-fallback #(malt-admin.controller.models/upload (assoc req :problems %))
    (let [values (fp/parse-params form/upload-form params)]
      {:body (pr-str (slurp (:tempfile (:file values))))})))