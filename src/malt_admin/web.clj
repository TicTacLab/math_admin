(ns malt-admin.web
  (:require [compojure.core :refer (defroutes GET POST PUT DELETE ANY wrap-routes)]
            [schema.core :as s]
            [org.httpkit.server :as http-kit]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.tools.trace :refer (trace)]
            [malt-admin.controller
             [models :as models]
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
            [malt-admin.config :as c]))


(defmacro allow [req role body]
  `(if (or (= ~role :any)
           (and (= ~role :admin)
                (get-in ~req [:session :is-admin])))
     ~body
     (render-error ~req 403)))


(defroutes routes
  (GET    "/" req (allow req :any (res/redirect "/models")))

  (GET    "/auth" req (allow req :any (auth/index req)))
  (POST   "/auth" req (allow req :any (auth/sign-in req)))
  (DELETE "/auth" req (allow req :any (auth/sign-out req)))

  (GET    "/files"                   req (allow req :any   (models/index req)))
  (GET    "/files/upload"            req (allow req :admin (models/upload req)))
  (GET    "/files/:id/edit"          req (allow req :admin (models/edit req)))
  (GET    "/files/:id/download"      req (allow req :admin (models/download req)))
  (GET    "/files/:id/:rev/profile"  req (allow req :any   (models/profile req)))
  (POST   "/files/:id/:rev/profile"  req (allow req :any   (models/profile-execute req)))
  (DELETE "/files/:id/:rev/session"  req (allow req :any   (models/delete-session req)))
  (POST   "/files/:id/log"           req (allow req :any   (models/read-log req)))
  (PUT    "/files/:id"               req (allow req :admin (models/replace req)))
  (DELETE "/files/:id"               req (allow req :admin (models/delete req)))
  (POST   "/files"                   req (allow req :admin (models/do-upload req)))

  (GET    "/filler"                       req (allow req :admin (filler/index req)))

  (GET    "/users"                        req (allow req :admin (users/index req)))
  (GET    "/users/new"                    req (allow req :admin (users/new* req)))
  (POST   "/users"                        req (allow req :admin (users/create req)))
  (GET    "/users/:login/edit"            req (allow req :admin (users/edit req)))
  (POST   "/users/pass-analyze"           req (allow req :admin (users/pass-analyze req)))
  (PUT    "/users/:login"                 req (allow req :admin (users/update req)))
  (GET    "/users/:login/edit-password"   req (allow req :admin (users/edit-password req)))
  (PUT    "/users/:login/update-password" req (allow req :admin (users/update-password req)))
  (PUT    "/users/:login/change-status"   req (allow req :admin (users/change-status req)))

  (GET "/static/401" req (render-error req 401))
  (GET "/static/403" req (render-error req 403))
  (GET "/static/404" req (render-error req 404))
  (GET "/static/500" req (render-error req 500))

  (ANY "/*" req (render-error req 404)))

(defn app [web]
  (-> routes
      (wrap-check-session)
      (wrap-csp)
      (wrap-csrf-cookie)
      (wrap-check-csrf)
      (wrap-resource "public")
      (wrap-webjars)
      (wrap-keyword-params)
      (wrap-multipart-params)
      (wrap-params)
      (wrap-flash)
      (wrap-session {:cookie-name "id"})
      (wrap-cookies)
      (wrap-no-cache-cookies)
      (wrap-with-web web)
      (wrap-with-stacktrace)))

(defrecord Web [host port api-addr server storage handler offloader]
  component/Lifecycle

  (start [component]
    (let [handler (app component)
          srv (http-kit/run-server handler {:port port
                                            :host host
                                            :max-body 52428800 ;; 50Mb
                                            :join? false})]
      (selmer.parser/set-resource-path! (clojure.java.io/resource "templates"))
      (if (= (:app-env @c/config) "production")
        (selmer.parser/cache-on!)
        (selmer.parser/cache-off!))

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
  {:port     s/Int
   :host     s/Str
   :api-addr s/Str})

(defn new-web [m]
  (as-> m $
        (select-keys $ (keys WebSchema))
        (s/validate WebSchema $)
        (map->Web $)))
