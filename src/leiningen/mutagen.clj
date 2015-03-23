(ns leiningen.mutagen
  (:require [environ.core :as environ])
  (:import [com.toddfast.mutagen.cassandra.impl CassandraMutagenImpl]
           [com.netflix.astyanax AstyanaxContext$Builder]
           [com.netflix.astyanax.impl AstyanaxConfigurationImpl]
           [com.netflix.astyanax.model ConsistencyLevel]
           [com.netflix.astyanax.connectionpool.impl ConnectionPoolConfigurationImpl]
           [com.netflix.astyanax.thrift ThriftFamilyFactory]))

(defn mutagen [_project & _args]
  (let [{:keys [storage-keyspace storage-nodes]} environ/env
        m (doto (CassandraMutagenImpl.)
            (.initialize "mutations"))
        context (-> (AstyanaxContext$Builder.)
                    (.forKeyspace storage-keyspace)
                    (.withAstyanaxConfiguration (-> (AstyanaxConfigurationImpl.)
                                                    (.setDefaultReadConsistencyLevel ConsistencyLevel/CL_ALL)
                                                    (.setDefaultWriteConsistencyLevel ConsistencyLevel/CL_ALL)))
                    (.withConnectionPoolConfiguration (-> (ConnectionPoolConfigurationImpl. "Your connection pool")
                                                          (.setPort 9160)
                                                          (.setMaxConnsPerHost 1)
                                                          (.setSeeds storage-nodes)))
                    (.buildKeyspace (ThriftFamilyFactory/getInstance)))
        _ (.start context)
        keyspace (.getClient context)
        result (.mutate m keyspace)]
    (when-let [exception (.getException result)]
      (throw exception))
    (if (.isMutationComplete result)
      (println "Migration successfully completed!")
      (do
        (println "Error during migration!")
        (println (pr-str result))))))
