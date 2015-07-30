(ns malt-admin.system
  (:require [com.stuartsierra.component :as component]
            [malt-admin.web :as web]
            [malt-admin.offloader :as off]
            [zabbix-clojure-agent.core :as zabbix]
            [malt-admin.storage :as storage]))

(defn new-system [config]
  (component/system-map
   :web (component/using
         (web/new-web config)
         [:storage :offloader])

   ;; TODO: make filler better
   #_:filler #_(component/using
             (filler/new-filler {})
             [:storage])
   :offloader (component/using
                (off/new-offloader config)
                [:storage])
   :storage (storage/new-storage config)
   :zabbix-reporter (zabbix/new-zabbix-reporter
                      {:hostname         (:monitoring-hostname config)
                       :zabbix-host      (:zabbix-host config)
                       :zabbix-port      (:zabbix-port config)
                       :interval-minutes 10})))