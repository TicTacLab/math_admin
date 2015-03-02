(ns malt-admin.web
  (:require [compojure.core :refer (defroutes GET POST PUT DELETE wrap-routes)]
            [schema.core :as s]
            [compojure.route :as route]
            [org.httpkit.server :as http-kit]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.tools.trace :refer (trace)]
            [malt-admin.controller
             [configuration :as configuration]
             [models :as models]
             [users :as users]]
            [malt-admin.view :refer (render)]
            [environ.core :as environ]
            [malt-admin.web.middleware :refer (wrap-check-session wrap-with-web wrap-with-stacktrace)]
            [ring.util.response :as res]
            [ring.middleware
             [cookies :refer (wrap-cookies)]
             [flash :refer (wrap-flash)]
             [session :refer (wrap-session)]
             [params :refer (wrap-params)]
             [webjars :refer (wrap-webjars)]
             [keyword-params :refer (wrap-keyword-params)]
             [multipart-params :refer (wrap-multipart-params)]]))

(defroutes routes
  (GET    "/" req (res/redirect "/configuration"))

  (GET    "/configuration" req (configuration/index req))
  (POST   "/configuration" req (configuration/update req))

  (GET    "/models" req (models/index req))
  (GET    "/models/upload" req (models/upload req))
  (GET    "/models/:id/edit" req (models/edit req))
  (GET    "/models/:id/download" req (models/download req))
  (PUT    "/models/:id" req (models/replace req))
  (DELETE "/models/:id" req (models/delete req))
  (POST   "/models" req (models/do-upload req))
  
  (GET    "/users" req (users/index req))
  (GET    "/users/new" req (users/new* req))
  (POST   "/users" req (users/create req))
  (GET    "/users/:login/edit" req (users/edit req))
  (PUT    "/users/:login" req (users/update req))
  (GET    "/users/:login/edit-password" req (users/edit-password req))
  (PUT    "/users/:login/update-password" req (users/update-password req))
  (PUT    "/users/:login/change-status" req (users/change-status req))

  (route/not-found "<h1>Page not found!</h1>"))

(defn app [web]
  (-> routes
      (wrap-webjars)
      (wrap-keyword-params)
      (wrap-multipart-params)
      (wrap-params)
      (wrap-flash)
      (wrap-session)
      (wrap-cookies)
      (wrap-with-web web)
      (wrap-with-stacktrace)))

(defrecord Web [host port server storage]
  component/Lifecycle

  (start [component]
    (let [srv (http-kit/run-server (app component) {:port port
                                                    :host host
                                                    :join? false})]
      (selmer.parser/set-resource-path! (clojure.java.io/resource "templates"))
      (if (= (:app-env environ/env) "production")
        (selmer.parser/cache-on!)
        (selmer.parser/cache-off!))

      (log/info "Web service started at:" (str host ":" port))
      (assoc component :server srv)))

  (stop [component]
    (when server
      (server)
      (log/info "Web service stopped"))
    (assoc component :server nil)))

(def WebSchema
  {:port s/Int
   :host s/Str})

(defn new-web [m]
  (s/validate WebSchema m)
  (map->Web m))
