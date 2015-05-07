(ns mutagen
  (:require [environ.core :as environ]
            [clojure.string :as string])
  (:import [com.toddfast.mutagen.cassandra.impl CassandraMutagenImpl]
           [com.netflix.astyanax AstyanaxContext$Builder]
           [com.netflix.astyanax.impl AstyanaxConfigurationImpl]
           [com.netflix.astyanax.model ConsistencyLevel]
           [com.netflix.astyanax.connectionpool.impl ConnectionPoolConfigurationImpl SimpleAuthenticationCredentials]
           [com.netflix.astyanax.thrift ThriftFamilyFactory])
  (:gen-class))

(defn -main [& _args]
  (let [{:keys [storage-keyspace storage-nodes
                storage-user storage-password]} environ/env
        m (doto (CassandraMutagenImpl.)
            (.initialize "mutations"))
        context (-> (AstyanaxContext$Builder.)
                    (.forKeyspace storage-keyspace)
                    (.withAstyanaxConfiguration (-> (AstyanaxConfigurationImpl.)
                                                    (.setDefaultReadConsistencyLevel ConsistencyLevel/CL_ALL)
                                                    (.setDefaultWriteConsistencyLevel ConsistencyLevel/CL_ALL)
                                                    (.setCqlVersion "3.1.1")
                                                    (.setTargetCassandraVersion "1.2")))
                    (.withConnectionPoolConfiguration (-> (ConnectionPoolConfigurationImpl. "Your connection pool")
                                                          (.setPort 9160)
                                                          (.setMaxConnsPerHost 1)
                                                          (.setSeeds (first (string/split storage-nodes #",")))
                                                          (.setAuthenticationCredentials (SimpleAuthenticationCredentials. storage-user
                                                                                                                           storage-password))))
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
