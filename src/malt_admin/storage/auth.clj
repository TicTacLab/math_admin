(ns malt-admin.storage.auth
  (:require [clojurewerkz.scrypt.core :as sc]
            [malt-admin.storage.users :as user]
            [yesql.core :refer [defqueries]]
            [dire.core :refer [with-handler!]]
            [clj-time.core :as time]
            [clj-time.coerce :refer [from-date]])
  (:import (java.util UUID Date)
           (java.sql Timestamp)))

(defqueries "sql/auth.sql")

(defn create-session! [{spec :spec} login]
  (let [session-id (UUID/randomUUID)]
    (create-session*! {:login      login
                       :session_id session-id
                       :last_used  (Timestamp. (.getTime (Date.)))}
                      {:connection spec})
    (str session-id)))

(defn update-session! [{spec :spec} session-id]
  (update-session*! {:session_id session-id
                     :last_used  (Timestamp. (.getTime (Date.)))}
                    {:connection spec}))

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
  (delete-session*! {:session_id session-id} {:connection spec}))


(defn is-valid-session? [{spec :spec ttl :session-ttl} session-id]
  (let [last-used (get-last-used* {:session_id (UUID/fromString session-id)} {:connection spec})
        elapsed (time/in-seconds (time/interval (from-date last-used) (from-date (Date.))))]
    (and last-used (<= elapsed ttl))))