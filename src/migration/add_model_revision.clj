(ns migration.add-model-revision
  (:require [com.stuartsierra.component :as component]
            [malt-admin.system :as sys]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as q]
            [malt-admin.config :as c])
  (:import (java.util UUID Date))
  (:gen-class))


(defn -main [& args]
  (let [storage (-> @c/config
                    sys/new-system
                    :storage
                    component/start)

        c (:conn storage)]
    (cql/alter-table c "models"
                     (q/add-column :rev :varchar))

    (->> (cql/select c "models" (q/columns :id))
         (map #(cql/update c "models"
                           {:rev (str (UUID/randomUUID))}
                           (q/where [[= :id (:id %)]])))
         doall)))
