(ns malt-admin.storage.settings
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))


(defn write-settings! [storage config]
  (let [{:keys [conn settings-table]} storage]
    (cql/truncate conn settings-table)
    (some->> config
             json/generate-string
             (hash-map :settings)
             (cql/insert conn settings-table))))

(defn read-settings [storage]
  (let [{:keys [conn settings-table]} storage]
    (try
      (json/parse-string (some->> (cql/select conn settings-table)
                                  first
                                  :settings)
                         true)
      (catch Exception e
        (log/error e "occured while reading config")))))
