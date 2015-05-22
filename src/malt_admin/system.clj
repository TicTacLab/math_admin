(ns malt-admin.system
  (:require [com.stuartsierra.component :as component]
            [malt-admin.web :as web]
            [malt-admin.storage :as storage]))

(defn new-system [config]
  (component/system-map
   :web (component/using
         (web/new-web config)
         [:storage])
   :storage (storage/new-storage config)))
