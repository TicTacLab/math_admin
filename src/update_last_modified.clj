(ns update-last-modified
  (:require [environ.core :as environ]
            [malt-admin.storage :as storage]
            [com.stuartsierra.component :as component]
            [malt-admin.storage.models :as models]
            [malt-admin.helpers :refer [csv-to-list]]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns order-by]])
  (:gen-class)
  (:import (java.util Date)))

 (defn -main [& args]
   (let [storage-system  (component/start (storage/new-storage {:storage-nodes       (csv-to-list (:storage-nodes environ/env))
                                                        :storage-keyspace    (:storage-keyspace environ/env)
                                                        :settings-table      (:settings-table environ/env)
                                                        :storage-user        (:storage-user environ/env)
                                                        :storage-password    (:storage-password environ/env)
                                                        :configuration-table (:configuration-table environ/env)
                                                        :session-ttl         (Integer/valueOf (:session-ttl environ/env))}))

         conn (:conn storage-system)]
     (->>
       (cql/select conn "models"(columns :id))
       (map #(assoc % :last_modified (Date.)))
       (map #(cql/update conn "models"
                        (set (dissoc % :id))
                        (where [[= :id (:id %)]])))
       doall)
     (component/stop storage-system)))
