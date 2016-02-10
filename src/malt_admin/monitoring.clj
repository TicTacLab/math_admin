(ns malt-admin.monitoring
  (:require [com.stuartsierra.component :as component]
            [metrics.core :refer [default-registry]])
  (:import (com.codahale.metrics JmxReporter)))

(defrecord JmxReporterComp [^JmxReporter reporter]
  component/Lifecycle
  (start [this]
    (let [reporter (.build (JmxReporter/forRegistry default-registry))]
      (.start reporter)
      (assoc this :reporter reporter)))

  (stop [this]
    (when reporter
      (.stop reporter))
    (assoc this :reporter nil)))

(defn new-jmx-reporter []
  (JmxReporterComp. nil))