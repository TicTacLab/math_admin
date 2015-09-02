(ns malt-admin.controller.settings
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as cfg]
            [malt-admin.form.settings :as forms]
            [malt-admin.audit :as audit]
            [formative.parse :as fp]
            [ring.util.response :as res]))

(defn read-version []
  (binding [*read-eval* false]
    (-> "project.clj"
        slurp
        read-string
        (nth 2))))

(defn index [{{storage :storage} :web
              params :params
              problems :problems
              :as req}]
  (let [settings (if (contains? params :submit)
                 (dissoc params :submit)
                 (cfg/read-settings storage))]

    (render "settings/index"
            req
            {:app-version (read-version)
             :form (assoc forms/settings
                     :values settings
                     :problems problems
                     :action "/settings"
                     :method "POST"
                     :submit-label "Update")})))

(defn update [{{storage :storage} :web
               params :params :as req}]
  (fp/with-fallback #(index (assoc req :problems %))
    (let [settings (fp/parse-params forms/settings params)]
      (cfg/write-settings! storage settings)
      (audit/info req :udpate-settings settings)
      (res/redirect-after-post "/settings"))))
