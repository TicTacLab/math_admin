(ns malt-admin.storage.users
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]))

(defn write-user! [{conn :conn} user]
  (let [defaults {:status "active" :is_admin false}]
    (cql/insert conn "users" (merge defaults user))))

(defn get-users [{conn :conn}]
  (cql/select conn "users"
              (columns :name :login :status :is_admin)))

(defn get-user [{conn :conn} login]
  (cql/get-one conn "users"
              (columns :login :name :is_admin)
              (where [[= :login login]])))

(defn update-user! [{conn :conn} login user]
  (cql/update conn "users" user
              (where [[= :login login]])))
