(ns malt-admin.storage.auth
  (:require [clojurewerkz.scrypt.core :as sc]
            [malt-admin.storage.users :as user]
            [malt-admin.storage :refer [sql-exception-handler]]
            [yesql.core :refer [defqueries]]
            [dire.core :refer [with-handler!]]
            [clj-time.core :as time]
            [clj-time.coerce :refer [from-date]])
  (:import (java.util UUID Date)
           (java.sql Timestamp SQLException)))

(defqueries "sql/auth.sql")

(defn create-session! [{spec :spec} login]
  (let [session-id (UUID/randomUUID)]
    (create-session*! {:login      login
                       :session_id session-id
                       :last_used  (Timestamp. (.getTime (Date.)))}
                      {:connection spec})
    (str session-id)))

(with-handler! #'create-session!
  SQLException
  sql-exception-handler)


(defn update-session! [{spec :spec} session-id]
  (update-session*! {:session_id (UUID/fromString session-id)
                     :last_used  (Timestamp. (.getTime (Date.)))}
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

(defn get-last-used [{spec :spec} session-id]
  (-> (get-last-used* {:session_id (UUID/fromString session-id)} {:connection spec})
      first
      :last_used))

(with-handler! #'get-last-used
  SQLException
  sql-exception-handler)

(defn is-valid-session? [{ttl :session-ttl :as st} session-id]
  (let [last-used (get-last-used st session-id)
        elapsed (time/in-seconds (time/interval (from-date last-used) (from-date (Date.))))]
    (and last-used (<= elapsed ttl))))