(ns migration.add-in-params
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.cassaforte.query :refer :all]
            [malt-admin.system :as sys]
            [clojurewerkz.cassaforte.cql :as cql]
            [malt-admin.config :as c])
  (:gen-class))

 (defn -main [& args]
   (let [storage (-> @c/config
                     sys/new-system
                     :storage
                     component/start)

         c (:conn storage)]
     (cql/create-table c "in_params"
                       (column-definitions {:model_id :int
                                            :params :blob
                                            :primary-key [:model_id :params]}))
     (component/stop storage)))