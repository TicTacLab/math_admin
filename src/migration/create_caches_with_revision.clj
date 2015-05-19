(ns migration.create-caches-with-revision
  (:require [environ.core :as environ]
            [com.stuartsierra.component :as component]
            [malt-admin.helpers :refer [csv-to-list]]
            [malt-admin.system :as sys]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as q])
  (:gen-class))


(defn -main [& args]
  (let [storage (-> environ/env
                    sys/new-system
                    :storage
                    component/start)

        c (:conn storage)]
    (cql/create-table c "caches"
                      (q/column-definitions
                        {:model_id    :int
                         :rev         :varchar
                         :params      :blob
                         :result      :blob
                         :primary-key [[:model_id :rev] :params]}))

    (->> (cql/select c "models" (q/columns :id :rev))
         (map (fn [{mid :id rev :rev}]
                (->> (cql/select c "cache" (q/where [[= :model_id mid]]))
                     (map #(assoc % :rev rev)))))
         (map #(cql/insert-batch c "caches" %))
         (doall))

    (cql/drop-table c "cache")))
