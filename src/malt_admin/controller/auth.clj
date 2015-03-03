(ns malt-admin.controller.auth
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [malt-admin.form.auth :as form]
            [malt-admin.helpers :refer [redirect-with-flash error!]]
            [malt-admin.storage.auth :as storage]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [clojurewerkz.scrypt.core :as sc]
            [formative.core :as f]
            [org.httpkit.client :as http]
            [clojure.tools.logging :as log]))

(defn index [{:keys [problems params]
              :as   req}]
  (render "auth/index" req {:form (assoc form/signin
                                    :problems problems
                                    :values (select-keys params [:login])
                                    :action "/auth"
                                    :method "POST")}))

(defn sign-in [{params             :params
               {storage :storage} :web
               :as                req}]
  (fp/with-fallback #(malt-admin.controller.auth/index (assoc req :problems %))
   (let [{:keys [login password]} (fp/parse-params form/signin params)]
     (if-let [session-id (storage/sign-in storage login password)]
       (-> (redirect-with-flash "/users" {:success "You successfully signed in"})
           (assoc-in [:session :sid] session-id))
       (error! [:login :password] "Invalid login or password")))))

(defn sign-out [{{storage :storage} :web
                 session-id :session-id}]
  (storage/sign-out storage session-id)
  (-> (redirect-with-flash "/auth" {:success "You successfully signed out"})
      (assoc-in [:session :sid] nil)))
