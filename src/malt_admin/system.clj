(ns malt-admin.system
  (:require [com.stuartsierra.component :as component]
            [malt-admin.web :as web]
            [malt-admin.filler :as filler]
            [zabbix-clojure-agent.core :as zabbix]
            [malt-admin.storage :as storage]))

(defn new-system [config]
  (component/system-map
   :web (component/using
         (web/new-web config)
         [:storage])
   :filler (component/using
             (filler/new-filler {})
             [:storage])
   :storage (storage/new-storage config)
   :zabbix-reporter (zabbix/new-zabbix-reporter
                      {:hostname         (:monitoring-hostname config)
                       :zabbix-host      (:zabbix-host config)
                       :zabbix-port      (:zabbix-port config)
                       :interval-minutes 10})))
