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
           storage-user
           storage-password
           settings-table
           configuration-table
           session-ttl] :as config}]

  (component/system-map
   :web (component/using
         (web/new-web {:host web-host
                       :port (Integer/valueOf web-port)})
         {:storage :storage})
   :filler (component/using
             (filler/new-filler
               {:configuration-table configuration-table})
             [:storage])
   :storage (storage/new-storage {:storage-nodes (csv-to-list storage-nodes)
                                  :storage-keyspace storage-keyspace
                                  :settings-table settings-table
                                  :storage-user storage-user
                                  :storage-password storage-password
                                  :configuration-table configuration-table
                                  :session-ttl (Integer/valueOf session-ttl)})))
