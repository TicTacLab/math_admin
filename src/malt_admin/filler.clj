(ns malt-admin.filler
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [clojure.tools.logging :as log]
            [malt-admin.storage.configuration :as cfg]))

(defn filler-handler [component]
  nil)

(defrecord Filler [storage addr filler-thread]
  component/Lifecycle
  (start [component]
    (let [component (assoc component :addr (->> storage
                                                cfg/read-settings
                                                :cache-filler-addr))
          filler-thread (Thread. (partial filler-handler component))]

      (log/info "Filler started")
      (assoc component :filler-thread filler-thread)))

  (stop [component]
    (when filler-thread
      (try
        (.interrupt filler-thread)
        (catch Exception _)))
    (log/info "Filler stopped")
    (assoc component :filler-thread nil)))


(defn new-filler [m]
  (map->Filler m))

