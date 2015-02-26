(ns malt-admin.web
  (:require [compojure.core :refer (defroutes GET POST wrap-routes)]
            [schema.core :as s]
            [compojure.route :as route]
            [org.httpkit.server :as http-kit]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.tools.trace :refer (trace)]
            [malt-admin.controller
             [configuration :as configuration]
             [models :as models]]
            [malt-admin.view :refer (render)]
            [environ.core :as environ]
            [malt-admin.web.middleware :refer (wrap-check-session wrap-with-web wrap-with-stacktrace)]
            [ring.util.response :as res]
            [ring.middleware
             [cookies :refer (wrap-cookies)]
             [session :refer (wrap-session)]
             [params :refer (wrap-params)]
             [webjars :refer (wrap-webjars)]
             [keyword-params :refer (wrap-keyword-params)]
             [multipart-params :refer (wrap-multipart-params)]]))

(defroutes routes
  (GET  "/" req (res/redirect "/configuration"))

  (GET  "/configuration" req (configuration/index req))
  (POST "/configuration" req (configuration/update req))

  (GET  "/models" req (models/index req))
  (GET "/models/upload" req (models/upload req))
  (wrap-multipart-params
    (POST "/models" req (models/do-upload req)))

  (route/not-found "<h1>Page not found!</h1>"))

(defn app [web]
  (-> routes
#_      (wrap-check-session)
      (wrap-webjars)
      (wrap-keyword-params)
      (wrap-params)
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
