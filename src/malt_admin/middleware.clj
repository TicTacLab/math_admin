(ns malt-admin.middleware
  (:require [ring.util.response :as res]
            [clojure.tools.logging :as log]
            [ring.middleware.stacktrace :as stacktrace]
            [malt-admin.view :refer (render render-error)]
            [malt-admin.storage.auth :as storage]
            [malt-admin.config :as c]))

(defmacro defmiddleware [nm params handler-params & body]
  `(defn ~nm ~params
     (fn ~handler-params
       ~@body)))

(defn allowed-for-all? [uri]
  (some #(re-find % uri)
        [#"^/$" #"^/auth$" #"^/static/*"]))

(defmiddleware wrap-check-session
  [h] [{uri :uri web :web :as req}]
  (let [session-id  (get-in req [:session :sid])
        login (storage/get-login (:storage web) session-id)]
    (cond
      (allowed-for-all? uri) (h req )
      login (do (storage/create-or-update-session! (:storage web) login session-id)
                (h (assoc req :session-id session-id)))
      :else (res/redirect "/auth"))))

(defn wrap-with-stacktrace [h]
  (if (not= (:app-env @c/config "production"))
    (stacktrace/wrap-stacktrace h)
    (fn [req]
      (try
        (h req)
        (catch SecurityException e
          (log/error e "Exception raised")
          (render-error req 403))
        (catch IllegalStateException e
          (log/error e "Exception raised")
          (render-error req 500))))))

(defmiddleware wrap-with-web
  [h web] [req]
  (h (assoc req :web web)))
