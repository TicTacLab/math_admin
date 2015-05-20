(ns malt-admin.storage.cache-q
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns count1]]
            [taoensso.nippy :as nippy])
  (:import (com.datastax.driver.core.utils Bytes)))

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

(defn get-task [{conn :conn}]
  (let [task (cql/get-one conn "cache_q"
                          (columns :model_id
                                   :rev
                                   :params)
                          )]
    (when task
      (cql/delete conn "cache_q"
                  (where [[= :model_id (:model_id task)]
                          [= :rev (:rev task)]
                          [= :params (:params task)]]))

      (update-in task [:params] #(->> %
                                      Bytes/getArray
                                      nippy/thaw)))))

