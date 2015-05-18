(ns migration.add-model-revision
  (:require [environ.core :as environ]
            [com.stuartsierra.component :as component]
            [malt-admin.helpers :refer [csv-to-list]]
            [malt-admin.system :as sys]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as q])
  (:import (java.util UUID Date))
  (:gen-class))


(defn -main [& args]
  (let [storage (-> environ/env
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
