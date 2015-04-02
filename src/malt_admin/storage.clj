(ns malt-admin.storage
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.policies :as cp]
            [clojure.tools.logging :as log]))


(defrecord Storage [conn
                    storage-nodes
                    storage-keyspace
                    settings-table
                    storage-user
                    storage-password
                    configuration-table]
  component/Lifecycle

  (start [component]
    (let [conn (cc/connect storage-nodes
                           storage-keyspace
                           {:credentials {:username storage-user
                                          :password storage-password}
                            :reconnection-policy (cp/constant-reconnection-policy 100)})]
      (log/info "Storage started")
      (assoc component :conn conn)))

  (stop [component]
    (when conn
      (cc/disconnect conn))
    (log/info "Storage stopped")
    (assoc component :conn nil)))


(def StorageSchema
  {:storage-nodes [s/Str]
   :storage-keyspace s/Str
   :storage-user s/Str
   :storage-password s/Str
   :settings-table s/Str
   :configuration-table s/Str})

(defn new-storage [m]
  (s/validate StorageSchema m)
  (map->Storage m))
