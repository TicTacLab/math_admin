(ns malt-admin.storage
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.policies :as cp]
            [clojure.tools.logging :as log])
  (:import [com.datastax.driver.core.exceptions NoHostAvailableException]
           (com.datastax.driver.core.policies DCAwareRoundRobinPolicy)))

(def sql-exception-handler
  (fn [e & args]
    (log/error e "Exception occured during file writing into db")))


(defn try-connect-times [times delay-ms nodes keyspace opts]
  (let [result (try
                 (cc/connect nodes keyspace opts)
                 (catch NoHostAvailableException ex ex))]
    (cond
      (and (instance? Exception result) (zero? times)) (throw result)
      (instance? Exception result) (do
                                     (log/warnf "Failed to connect to Cassandra, will retry after %d ms" delay-ms)
                                     (Thread/sleep delay-ms)
                                     (recur (dec times) delay-ms nodes keyspace opts))
      :else result)))

(defrecord Storage [conn
                    storage-nodes
                    storage-keyspace
                    storage-user
                    storage-password
                    storage-default-dc
                    storage-pg-host
                    storage-pg-user
                    storage-pg-pass
                    storage-pg-db
                    pg-spec
                    session-ttl]
  component/Lifecycle

  (start [component]
    (let [conn (try-connect-times
                 1000
                 1000
                 storage-nodes
                 storage-keyspace
                 {:credentials           {:username storage-user
                                          :password storage-password}
                  :reconnection-policy   (cp/constant-reconnection-policy 100)
                  :load-balancing-policy (DCAwareRoundRobinPolicy. storage-default-dc 2)})
          pg-spec {:classname "org.postgresql.Driver"
                   :subprotocol "postgresql"
                   :subname (format "//%s/%s" storage-pg-host storage-pg-db)
                   :user storage-pg-user
                   :password storage-pg-pass}]
      (log/info "Storage started")
      (assoc component :conn conn :pg-spec pg-spec)))

  (stop [component]
    (when conn
      (cc/disconnect conn))
    (when pg-spec
      #_(.close pg-spec))
    (log/info "Storage stopped")
    (assoc component :conn nil :pg-spec nil)))


(def StorageSchema
  {:storage-nodes [s/Str]
   :storage-keyspace s/Str
   :storage-user s/Str
   :storage-password s/Str
   :storage-default-dc s/Str
   :session-ttl s/Int
   :storage-pg-host s/Str
   :storage-pg-user s/Str
   :storage-pg-pass s/Str
   :storage-pg-db s/Str})

(defn new-storage [m]
  (as-> m $
        (select-keys $ (keys StorageSchema))
        (s/validate StorageSchema $)
        (map->Storage $)))
