(ns malt-admin.embedded-storage
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql :as cql])
  (:import (org.cassandraunit.utils EmbeddedCassandraServerHelper)
           (org.cassandraunit CQLDataLoader)
           (org.cassandraunit.dataset.cql FileCQLDataSet)))

(defrecord EmbeddedStorage [conn
                            storage-nodes
                            storage-keyspace
                            configuration-table
                            settings-table]
  component/Lifecycle
  (start [component]
    (EmbeddedCassandraServerHelper/startEmbeddedCassandra)
    (EmbeddedCassandraServerHelper/cleanEmbeddedCassandra)
    (let [conn (cc/connect ["localhost"] {:port 9142})]
      (doto (CQLDataLoader. conn)
        (.load (FileCQLDataSet. "scripts/init-schema.cql" true false storage-keyspace))
        (.load (FileCQLDataSet. "scripts/init-config.cql" false false storage-keyspace))
        (.load (FileCQLDataSet. "scripts/init-settings.cql" false false storage-keyspace)))
      (cql/use-keyspace conn storage-keyspace)
      (assoc component :conn conn)))
  
  (stop [component]
    (when conn
      (cc/disconnect conn))
    (assoc component :conn nil)))