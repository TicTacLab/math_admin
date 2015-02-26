(ns malt-admin.storage.configuration
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))


(defn write-config! [storage config]
  (let [{:keys [conn configuration-table]} storage]
    (cql/truncate conn configuration-table)
    (some->> config
             json/generate-string
             (hash-map :config)
             (cql/insert conn configuration-table))))

(defn read-config [storage]
  (let [{:keys [conn configuration-table]} storage]
    (try
      (json/parse-string (some->> (cql/select conn configuration-table)
                                  first
                                  :config)
                         true)
      (catch Exception e
        (log/error e "occured while reading config")))))
