(ns malt-admin.filler
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [clojure.tools.logging :as log]
            [malt-admin.storage.configuration :as cfg]
            [malt-admin.storage.cache-q :as cache-q]
            [org.httpkit.client :as http]
            [metrics.gauges :as gauge]
            [aprint.core :refer [aprint]]
            [cheshire.core :as json]))

(def cache-queue-empty-retry 10000)
(def malt-failed-calculation-timeout 10000)

(defn calculate
  "calculation is
  successful on ANY http status 200/404/500 etc
  and failed on ANY error"
  [addr ssid id params]
  (let [url (format "http://%s/model/calc/%s" addr ssid)
        malt-params {:id     id
                     :ssid   ssid
                     :params params}
        {error :error} @(http/post url {:body    (json/generate-string malt-params)
                                         :headers {"Content-type" "text/plain"}
                                         :timeout 60000
                                         :as      :byte-array})]
    (if error
      (do
        (log/error error "While malt calculation")
        false)
      true)))

(defn gen-session-id [{model-id :model_id rev :rev}]
  (format "worker-%s-%s" model-id rev))

(defn filler-handler [{storage :storage addr :addr}]
  (loop []
    (let [result (try
                   (if-let [task (cache-q/get-task storage)]
                     (loop []
                       (when-not (calculate addr (gen-session-id task) (:model_id task) (:params task))
                         (Thread/sleep malt-failed-calculation-timeout)
                         (recur)))
                     (Thread/sleep cache-queue-empty-retry))
                   (catch Exception e
                     (log/error e)
                     (Thread/sleep malt-failed-calculation-timeout)
                     e))]
      (when-not (instance? InterruptedException result)
        (recur))))
  (log/info "Filler handler exited."))

(defrecord Filler [storage addr filler-thread cache-q-counter]
  component/Lifecycle
  (start [component]
    (let [component (assoc component :addr (->> storage
                                                cfg/read-settings
                                                :cache-filler-addr))
          filler-thread (Thread. (partial filler-handler component))]
      (.start filler-thread)

      (log/info "Filler started")
      (assoc component :filler-thread filler-thread
                       :cache-q-counter (gauge/gauge-fn ["malt_admin" "cache_queue_count"]
                                                        #(double (cache-q/get-queue-count (:storage component)))))))

  (stop [component]
    (when filler-thread
      (try
        (.interrupt filler-thread)
        (catch Exception _)))
    (log/info "Filler stopped")
    (assoc component :filler-thread nil
                     :cache-q-counter nil)))


(defn new-filler [m]
  (map->Filler m))

