(ns malt-admin.controller.auth
  (:require [malt-admin.view :refer (render)]
            [malt-admin.form.auth :as form]
            [malt-admin.helpers :refer [redirect-with-flash error!]]
            [malt-admin.storage.auth :as storage]
            [formative.parse :as fp]
            [ring.util.response :as res]))

(defn index [{:keys [problems params session-id]
              :as   req}]
  (if session-id
      (res/redirect "/")                                    ;; if already signed in, do nothing
      (render "auth/index" req {:form (assoc form/signin
                                        :problems problems
                                        :values (select-keys params [:login])
                                        :action "/auth"
                                        :method "POST")})))

(defn sign-in [{params             :params
               {storage :storage} :web
                session-id :session-id
               :as                req}]
  (if session-id
    (res/redirect "/")                                      ;; if already signed in, do nothing
    (fp/with-fallback #(malt-admin.controller.auth/index (assoc req :problems %))
      (let [{:keys [login password]} (fp/parse-params form/signin params)]
        (if-let [session-data (storage/sign-in storage login password)]
          (-> (redirect-with-flash "/mengine/files" {:success "You successfully signed in"})
              (assoc :session session-data))
          (error! [:login :password] "Invalid login or password"))))))

(defn sign-out [{{storage :storage} :web
                 session-id :session-id}]
  (when session-id
    (storage/sign-out storage session-id))
  (-> (redirect-with-flash "/auth" {:success "You successfully signed out"})
      (assoc-in [:session :sid] nil)))
