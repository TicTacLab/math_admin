(ns malt-admin.storage
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [clojure.tools.logging :as log]))

(def sql-exception-handler
  (fn [e & args]
    (log/error e "Exception occured during file writing into db")))

(defrecord Storage [storage-host
                    storage-user
                    storage-pass
                    storage-db
                    session-ttl
                    spec]
  component/Lifecycle

  (start [component]
    (let [spec {:classname   "org.postgresql.Driver"
                :subprotocol "postgresql"
                :subname     (format "//%s/%s" storage-host storage-db)
                :user        storage-user
                :password    storage-pass}]
      (log/info "Storage started")
      (assoc component :spec spec)))

  (stop [component]
    (log/info "Storage stopped")
    (assoc component :spec nil)))


(def StorageSchema
  {:session-ttl  s/Int
   :storage-host s/Str
   :storage-user s/Str
   :storage-pass s/Str
   :storage-db   s/Str})

(defn new-storage [m]
  (as-> m $
        (select-keys $ (keys StorageSchema))
        (s/validate StorageSchema $)
        (map->Storage $)))
