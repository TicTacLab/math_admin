(ns malt-admin.system
  (:require [com.stuartsierra.component :as component]
            [malt-admin.web :as web]))

(defn new-system
  [{:keys [web-port web-host c*-con-str] :as config}]

  (component/system-map
   :web (web/new-web {:host web-host
                      :port (Integer. web-port)})))
