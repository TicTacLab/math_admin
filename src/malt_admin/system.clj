(ns malt-admin.system
  (:require [com.stuartsierra.component :as component]
            [malt-admin.web :as web]
            [malt-admin.storage :as storage]
            [malt-admin.filler :as filler]
            [zabbix-clojure-agent.core :as zabbix]
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
           monitoring-hostname
           zabbix-host
           zabbix-port
           session-ttl] :as config}]

  (component/system-map
    :web (component/using
           (web/new-web {:host web-host
                         :port (Integer/valueOf web-port)})
           {:storage :storage})

    :filler (component/using
              (filler/new-filler {})
              [:storage])
    :storage (storage/new-storage {:storage-nodes       (csv-to-list storage-nodes)
                                   :storage-keyspace    storage-keyspace
                                   :settings-table      settings-table
                                   :storage-user        storage-user
                                   :storage-password    storage-password
                                   :configuration-table configuration-table
                                   :session-ttl         (Integer/valueOf session-ttl)})
    :zabbix-reporter (zabbix/new-zabbix-reporter
                       {:hostname         monitoring-hostname
                        :zabbix-host      zabbix-host
                        :zabbix-port      (Integer/valueOf zabbix-port)
                        :interval-minutes 10})))



