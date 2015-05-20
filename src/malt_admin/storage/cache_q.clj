(ns malt-admin.storage.cache-q
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns count1]]))

(defn insert-in-params! [storage rev-id in-params]
  (let [{:keys [conn]} storage]
    (->> in-params
         (map #(assoc % :rev rev-id))
         (cql/insert-batch conn "cache_q"))))

(defn delete! [{conn :conn} model-id rev-id]
  (cql/delete conn "cache_q"
              (where [[= :model_id model-id]
                      [= :rev rev-id]])))

(defn get-queue-count [{conn :conn}]
  (cql/perform-count conn "cache_q"))
