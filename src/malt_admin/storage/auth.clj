(ns malt-admin.storage.auth
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns using]]
            [clojurewerkz.scrypt.core :as sc]
            [clojure.set :refer [rename-keys]]
            [malt-admin.storage.users :as user])
  (:import (java.util UUID)))

(defn get-login-by-session-id [{conn :conn} session-id]
  (:login (cql/get-one conn "sessions"
                       (where [[= :session_id (UUID/fromString session-id)]]))))

(defn create-session! [{:keys [conn session-ttl]} login]
  (let [session-id (UUID/randomUUID)]
    (cql/insert conn "sessions" {:login      login
                                 :session_id session-id}
                (using :ttl session-ttl))
    (str session-id)))

(defn update-session! [{:keys [conn session-ttl] :as st} session-id]
  (let [login (get-login-by-session-id st session-id)]
    (cql/insert conn "sessions" {:login      login
                                 :session_id (UUID/fromString session-id)}
                (using :ttl session-ttl))
    session-id))

(defn sign-in [storage login password]
  (let [{:keys [status login is_admin] phash :password :as user} (user/get-user-with-password storage login)]
    (when (and user
               (= status "active")
               (sc/verify password phash))
      (hash-map
        :login login
        :is-admin is_admin
        :sid (create-session! storage login)))))

(defn sign-out [{conn :conn} session-id]
  (cql/delete conn "sessions"
              (where [[= :session_id (UUID/fromString session-id)]])))


