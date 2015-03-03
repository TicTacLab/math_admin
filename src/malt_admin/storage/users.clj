(ns malt-admin.storage.users
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [clojure.tools.logging :as log]))

(defn write-user! [{conn :conn} user]
  (let [defaults {:status "active"}]
    (cql/insert conn "users" (merge defaults user))))

(defn get-users [{conn :conn}]
  (cql/select conn "users"
              (columns :name :login :status)))

(defn get-user [{conn :conn} login]
  (first (cql/select conn "users"
                     (columns :login :name)
                     (where [[= :login login]]))))

(defn update-user! [{conn :conn} login user]
  (cql/update conn "users" user
              (where [[= :login login]])))