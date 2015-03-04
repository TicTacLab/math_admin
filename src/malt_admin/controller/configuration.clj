(ns malt-admin.controller.configuration
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [malt-admin.form.configuration :as forms]
            [malt-admin.audit :refer [audit]]
            [formative.parse :as fp]
            [formative.core :as fc]
            [ring.util.response :as res]))

(defn index [{{storage :storage} :web
              params :params
              problems :problems
              :as req}]
  (let [config (if (contains? params :submit)
                 (dissoc params :submit)
                 (-> storage
                     st/read-config
                     (update-in [:session-ttl] / 60)))]

    (render "configuration/index"
            req
            {:form (assoc forms/config
                     :values config
                     :problems problems
                     :action "/configuration"
                     :method "POST"
                     :submit-label "Update")})))

(defn update [{{storage :storage} :web
               params :params :as req}]
  (fp/with-fallback #(index (assoc req :problems %))
    (let [config (-> (fp/parse-params forms/config params)
                     (update-in [:session-ttl] * 60))]
      (clojure.pprint/pprint config)
      (st/write-config! storage config)
      (audit req :udpate-configuration config)
      (res/redirect-after-post "/configuration"))))
