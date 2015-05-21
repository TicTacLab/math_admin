(ns malt-admin.storage.configuration
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))


(defn- write-configuration! [conn table column value]
  (cql/truncate conn table)
  (->> value
       json/generate-string
       (hash-map column)
       (cql/insert conn table)))

(defn read-configuration [conn table column]
  (try
    (-> (cql/get-one conn table)
        (get column)
        (json/parse-string true))
    (catch Exception e
      (log/error e "occured while reading configuration"))))

(defn write-settings! [storage settings]
  (let [{:keys [conn settings-table]} storage]
    (write-configuration! conn settings-table :settings settings)))


(defn read-settings [storage]
  (let [{:keys [conn settings-table]} storage]
    (read-configuration conn settings-table :settings)))
