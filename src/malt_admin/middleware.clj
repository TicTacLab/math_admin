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

(defn session-syntax? [session-id]
  (try
    (UUID/fromString session-id)
    (catch IllegalArgumentException _
      false)
    (catch NullPointerException _
      false)))

(defmiddleware wrap-check-session
  [h] [{uri :uri web :web :as req}]
  (let [session-id  (get-in req [:session :sid])]
    (cond
      (allowed-for-all? uri)
      (h req)

      (nil? session-id)
      (res/redirect "/auth")

      (not (session-syntax? session-id))
      (do (audit/warn req :invalid-session-id session-id)
          (render-error req 403))

      (storage/get-login (:storage web) session-id)
      (do (storage/update-session! (:storage web) session-id)
          (h (assoc req :session-id session-id))))))

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
