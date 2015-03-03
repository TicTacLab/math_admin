(ns malt-admin.system
  (:require [com.stuartsierra.component :as component]
            [malt-admin.web :as web]
            [malt-admin.storage :as storage]
            [malt-admin.helpers :refer [csv-to-list]]))

(defn new-system
  [{:keys [web-port
           web-host
           storage-nodes
           storage-keyspace
           settings-table
           configuration-table] :as config}]

  (component/system-map
   :web (component/using
         (web/new-web {:host web-host
                       :port (Integer. web-port)})
         {:storage :storage})
   :storage (storage/new-storage {:storage-nodes (csv-to-list storage-nodes)
                                  :storage-keyspace storage-keyspace
                                  :settings-table settings-table
                                  :configuration-table configuration-table})))
