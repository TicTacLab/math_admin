(ns malt-admin.storage.configuration
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [clojure.tools.logging :as log]))


(defn get-profiles [storage]
  (let [{:keys [conn configuration-table]} storage]
    (->> (columns :profile)
         (cql/select conn configuration-table)
         (map :profile))))

(defn write-config! [storage profile config]
  (let [{:keys [conn configuration-table]} storage]
    (some->> config
             pr-str
             (hash-map :profile profile :config)
             (cql/insert conn configuration-table))))

(defn read-config [storage profile]
  (let [{:keys [conn configuration-table]} storage]
    (try
      (some->> {:profile profile}
               where
               (cql/select conn configuration-table)
               first
               :config
               read-string)
      (catch Exception e
        (log/error e "occured while reading config")))))

(defn delete-config! [storage profile]
  (let [{:keys [conn configuration-table]} storage]
    (cql/delete conn configuration-table (where {:profile profile}))))
