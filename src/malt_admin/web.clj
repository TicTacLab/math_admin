(ns malt-admin.web
  (:require [compojure.core :refer (defroutes GET POST PUT DELETE ANY wrap-routes)]
            [schema.core :as s]
            [org.httpkit.server :as http-kit]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [malt-admin.controller
             [mengine-files :as mengine]
             [sengine-files :as sengine]
             [sengine-events :as sengine-events]
             [live-viewer :as live-viewer]
             [users :as users]
             [filler :as filler]
             [auth :as auth]]
            [malt-admin.view :refer (render render-error)]
            [malt-admin.middleware :refer :all]
            [ring.util.response :as res]
            [ring.middleware
             [resource :refer (wrap-resource)]
             [cookies :refer (wrap-cookies)]
             [flash :refer (wrap-flash)]
             [session :refer (wrap-session)]
             [params :refer (wrap-params)]
             [webjars :refer (wrap-webjars)]
             [keyword-params :refer (wrap-keyword-params)]
             [multipart-params :refer (wrap-multipart-params)]]
            [ring.middleware.multipart-params.temp-file :refer (temp-file-store)]
            [malt-admin.config :as c]))


(defmacro allow [req role body]
  `(if (or (= ~role :any)
           (and (= ~role :admin)
                (get-in ~req [:session :is-admin])))
     ~body
     (render-error ~req 403)))


(defroutes routes
  (GET "/" req (allow req :any (res/redirect "/mengine/files")))

  (GET "/auth" req (allow req :any (auth/index req)))
  (POST "/auth" req (allow req :any (auth/sign-in req)))
  (DELETE "/auth" req (allow req :any (auth/sign-out req)))

  (GET "/mengine/files" req (allow req :any (mengine/index req)))
  (GET "/mengine/files/upload" req (allow req :admin (mengine/upload req)))
  (GET "/mengine/files/:id/edit" req (allow req :admin (mengine/edit req)))
  (GET "/mengine/files/:id/download" req (allow req :admin (mengine/download req)))
  (GET "/mengine/files/:id/:rev/profile" req (allow req :any (mengine/profile req)))
  (POST "/mengine/files/:id/:rev/profile" req (allow req :any (mengine/profile-execute req)))
  (DELETE "/mengine/files/:id/:rev/session" req (allow req :any (mengine/delete-session req)))
  (POST "/mengine/files/:id/log" req (allow req :any (mengine/read-log req)))
  (PUT "/mengine/files/:id" req (allow req :admin (mengine/replace req)))
  (DELETE "/mengine/files/:id" req (allow req :admin (mengine/delete req)))
  (POST "/mengine/files" req (allow req :admin (mengine/do-upload req)))

  (GET "/sengine/files" req (allow req :any (sengine/index req)))
  (GET "/sengine/files/upload" req (allow req :admin (sengine/upload req)))
  (POST "/sengine/files" req (allow req :admin (sengine/do-upload req)))
  (GET "/sengine/files/:id/edit" req (allow req :admin (sengine/edit req)))
  (POST "/sengine/files/:id/edit" req (allow req :admin (sengine/do-edit req)))
  (GET "/sengine/files/:id/download" req (allow req :admin (sengine/download req)))
  (DELETE "/sengine/files/:id" req (allow req :admin (sengine/delete req)))
  (GET "/sengine/files/:id/profile" req (allow req :any (sengine/init-profile-session req)))
  (GET "/sengine/live_viewer/:id" req (allow req :any (live-viewer/index req)))
  (GET "/sengine/files/profile/:event-id" req (allow req :any (sengine/view-profile req)))
  (POST "/sengine/files/profile/:event-id" req (allow req :any (sengine/send-profile req)))
  (POST "/sengine/files/profile/:event-id/destroy" req (allow req :any (sengine/destroy-profile-session req)))
  (POST "/sengine/files/profile/:event-id/workbook" req (allow req :admin (sengine/get-profile-workbook req)))

  (GET "/sengine/events" req (allow req :any (sengine-events/index req)))

  (GET "/filler" req (allow req :admin (filler/index req)))

  (GET "/users" req (allow req :admin (users/index req)))
  (GET "/users/new" req (allow req :admin (users/new* req)))
  (POST "/users" req (allow req :admin (users/create req)))
  (GET "/users/:login/edit" req (allow req :admin (users/edit req)))
  (POST "/users/pass-analyze" req (allow req :admin (users/pass-analyze req)))
  (PUT "/users/:login" req (allow req :admin (users/update req)))
  (GET "/users/:login/edit-password" req (allow req :admin (users/edit-password req)))
  (PUT "/users/:login/update-password" req (allow req :admin (users/update-password req)))
  (PUT "/users/:login/change-status" req (allow req :admin (users/change-status req)))

  (GET "/static/401" req (render-error req 401))
  (GET "/static/403" req (render-error req 403))
  (GET "/static/404" req (render-error req 404))
  (GET "/static/500" req (render-error req 500))

  (ANY "/*" req (render-error req 404)))

(defn app [web]
  (-> routes
      (wrap-check-session)
      #_(wrap-csp)
      (wrap-csrf-cookie)
      (wrap-check-csrf)
      (wrap-resource "public")
      (wrap-webjars)
      (wrap-keyword-params)
      (wrap-multipart-params :store (temp-file-store))
      (wrap-params)
      (wrap-flash)
      (wrap-session {:cookie-name "id"})
      (wrap-cookies)
      (wrap-no-cache-cookies)
      (wrap-with-web web)
      (wrap-with-stacktrace)))

(defrecord Web [host port m-engine-api-addr s-engine-api-addr server storage handler]
  component/Lifecycle

  (start [component]
    (let [handler (app component)
          srv (http-kit/run-server handler {:port port
                                            :host host
                                            :max-body 52428800 ;; 50Mb
                                            :join? false})]
      (selmer.parser/set-resource-path! (clojure.java.io/resource "templates"))
      (if (= (:app-env (c/config)) "dev")
        (selmer.parser/cache-off!)
        (selmer.parser/cache-on!))

      (log/info "Web service started at:" (str host ":" port))
      (assoc component
        :server srv
        :handler handler)))

  (stop [component]
    (when server
      (server)
      (log/info "Web service stopped"))
    (assoc component
      :server nil
      :handler nil)))

(def WebSchema
  {:port              s/Int
   :host              s/Str
   :m-engine-api-addr s/Str
   :s-engine-api-addr s/Str})

(defn new-web [m]
  (as-> m $
        (select-keys $ (keys WebSchema))
        (s/validate WebSchema $)
        (map->Web $)))
