(ns malt-admin.storage.auth
  (:require [clojurewerkz.scrypt.core :as sc]
            [malt-admin.storage.users :as user]
            [malt-admin.storage :refer [sql-exception-handler]]
            [yesql.core :refer [defqueries]]
            [dire.core :refer [with-handler!]]
            [clj-time.core :as time]
            [clj-time.coerce :refer [from-date to-timestamp]])
  (:import (java.util UUID Date)
           (java.sql Timestamp SQLException)))

(defqueries "sql/auth.sql")

(defn expire-time [ttl]
  (let [now (Date.)]
    (-> (Date.)
        (from-date)
        (time/plus (time/seconds ttl))
        (to-timestamp))))

(defn create-session! [{spec :spec ttl :session-ttl} login]
  (let [session-id (UUID/randomUUID)]
    (create-session*! {:login      login
                       :session_id session-id
                       :expire  (expire-time ttl)}
                      {:connection spec})
    (str session-id)))

(with-handler! #'create-session!
  SQLException
  sql-exception-handler)


(defn update-session! [{spec :spec ttl :session-ttl} session-id]
  (update-session*! {:session_id (UUID/fromString session-id)
                     :expire     (expire-time ttl)}
                    {:connection spec}))

(with-handler! #'update-session!
  SQLException
  sql-exception-handler)

(defn sign-in [storage login password]
  (let [{:keys [status login is_admin] phash :password :as user} (user/get-user-with-password storage login)]
    (when (and user
               (= status "active")
               (sc/verify password phash))
      (hash-map
        :login login
        :is-admin is_admin
        :sid (create-session! storage login)))))

(defn sign-out [{spec :spec} session-id]
  (delete-session*! {:session_id (UUID/fromString session-id)} {:connection spec}))

(with-handler! #'sign-out
  SQLException
  sql-exception-handler)

(defn get-expire [{spec :spec} session-id]
  (-> (get-expire* {:session_id (UUID/fromString session-id)} {:connection spec})
      first
      :expire))

(with-handler! #'get-expire
  SQLException
  sql-exception-handler)

(defn is-valid-session? [st session-id]
  (let [expire (get-expire st session-id)]
    (time/after? expire (from-date (Date.)))))