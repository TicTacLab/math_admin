(ns malt-admin.middleware
  (:require [ring.util.response :as res]
            [clojure.tools.logging :as log]
            [ring.middleware.stacktrace :as stacktrace]
            [malt-admin.view :refer (render)]
            [environ.core :as environ]))

(defmacro defmiddleware [nm params handler-params & body]
  `(defn ~nm ~params
     (fn ~handler-params
       ~@body)))

(defn non-authorizible? [uri]
  (some #(re-find % uri)
        [#"^/$" #"^/auth.*" #"^/static/.*"]))

(defmiddleware wrap-check-session
  [h] [{uri :uri :as req}]
  (let [session-id  (get-in req [:session :sid])]
    (if (or (non-authorizible? uri)
            session-id)
      (h (assoc req :session-id session-id))
      (res/redirect "/auth"))))

(defn wrap-with-stacktrace [h]
  (if (not= (:app-env environ/env) "production")
    (stacktrace/wrap-stacktrace h)
    (fn [req]
      (try
        (h req)
        (catch SecurityException e
          (log/error e "Exception raised")
          {:status 403
           :body (render "errors/403" req {})})
        (catch IllegalStateException e
          (log/error e "Exception raised")
          {:status 500
           :body (render "errors/500" req {})})))))

(defmiddleware wrap-with-web
  [h web] [req]
  (h (assoc req :web web)))
