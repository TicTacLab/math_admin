(ns malt-admin.storage.auth
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [clojurewerkz.scrypt.core :as sc])
  (:import (java.util UUID)))

(defn create-session! [{conn :conn} login]
  (let [session-id (UUID/randomUUID)]
    (cql/insert conn "sessions" {:login      login
                                 :session_id session-id})
    session-id))

(defn sign-in [{conn :conn :as storage} login password]
  (let [{phash :password status :status} (-> (cql/select conn "users"
                                                         (columns :password :status)
                                                         (where [[= :login login]]))
                                             first)]
    (when (and phash
               (= status "active")
               (sc/verify password phash))
      (create-session! storage login))))

(defn sign-out [{conn :conn} session-id]
  (cql/delete conn "sessions"
              (where [[= :session_id session-id]])))
