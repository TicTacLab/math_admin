(ns malt-admin.middleware
  (:require [ring.util.response :as res]
            [clojure.tools.logging :as log]
            [ring.middleware.stacktrace :as stacktrace]
            [malt-admin.view :refer (render render-error)]
            [malt-admin.storage.auth :as storage]
            [malt-admin.config :as c]
            [malt-admin.audit :as audit])
  (:import (java.util UUID)))

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

(defn is-form-post? [req]
  (and (= :post (:request-method req))
       (#{"application/x-www-form-urlencoded" "multipart/form-data"}
         (get-in req [:headers "content-type"]))))

(defn csrf-tokens-match? [req]
  (let [cookie-token (get-in req [:cookies "csrf" :value])
        post-token   (get-in req [:form-params "csrf"])]
    (= cookie-token post-token)))

(defmiddleware wrap-csrf-cookie [h] [req]
  (let [cookie (get-in req [:cookies "csrf"] (str (UUID/randomUUID)))]
    (assoc-in (h req) [:cookies "csrf"] cookie)))

(defmiddleware wrap-check-csrf [h] [req]
  (if (is-form-post? req)
    (if (csrf-tokens-match? req)
      (h req)
      (do
        (audit/warn req :csrf-token-don't-match (get-in req [:form-params "csrf"]))
        (render-error req 403)))
    (h req)))
