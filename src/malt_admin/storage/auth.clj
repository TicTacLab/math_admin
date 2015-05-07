(ns malt-admin.storage.auth
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns using]]
            [clojurewerkz.scrypt.core :as sc]
            [clojure.set :refer [rename-keys]])
  (:import (java.util UUID)))

(def user-ttl 30)

(defn create-or-update-session! [{conn :conn} login & [session-id]]
  (let [session-id (or session-id (UUID/randomUUID))]
    (cql/insert conn "sessions" {:login      login
                                 :session_id session-id}
                (using :ttl user-ttl))
    session-id))

(defn sign-in [{conn :conn :as storage} login password]
  (let [{:keys [status login is_admin] phash :password :as user} (-> (cql/select conn "users"
                                                                          (columns :password :status :is_admin :login)
                                                                          (where [[= :login login]]))
                                                              first)]
    (when (and user
               (= status "active")
               (sc/verify password phash))
      (some->> (create-or-update-session! storage login)
               (hash-map :login login :is-admin is_admin :sid)))))

(defn sign-out [{conn :conn} session-id]
  (cql/delete conn "sessions"
              (where [[= :session_id session-id]])))


(defn get-login [{conn :conn} session-id]
  (when session-id
    (some->> (cql/select conn "sessions"
                         (where [[= :session_id session-id]]))
             first
             :login)))