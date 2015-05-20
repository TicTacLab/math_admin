(ns malt-admin.filler
  (:require [com.stuartsierra.component :as component]
            [schema.core :as s]
            [clojure.tools.logging :as log]
            [malt-admin.storage.configuration :as cfg]
            [malt-admin.storage.cache-q :as cache-q]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))

(def filler-sleep 10000)

(defn calculate [addr ssid id params]
  (try
    (let [url (format "http://%s/model/calc/%s" addr ssid)
          malt-params {:id     id
                       :ssid   ssid
                       :params (:params params)}

          json-malt-params (json/generate-string malt-params)
          {:keys [body error status]} @(http/post url {:body json-malt-params
                                                       :headers {"Content-type" "text/plain"}
                                                       :timeout 60000
                                                       :as      :byte-array})]
      (when error (throw error))
      (when-not (= status 200)
        (throw (RuntimeException. (format "Bad Status Code: %d" status))))
      true)
    (catch Exception e
      (log/error e "While malt calculation")
      false)))

(defn gen-session-id [{model-id :model_id rev :rev}]
  (format "worker-%s-%s" model-id rev))

(defn filler-handler [{storage :storage addr :addr}]
  (while true
    (let [task (cache-q/get-task storage)]
                (if task
                  (do
                    (log/info "calc next task")
                    (calculate addr (gen-session-id task) (:model_id task) (:params task)))
                  (do
                    (log/info "calc sleep")
                    (Thread/sleep filler-sleep))))))

(defrecord Filler [storage addr filler-thread]
  component/Lifecycle
  (start [component]
    (let [component (assoc component :addr (->> storage
                                                cfg/read-settings
                                                :cache-filler-addr))
          filler-thread (Thread. (partial filler-handler component))]
      (.start filler-thread)

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

