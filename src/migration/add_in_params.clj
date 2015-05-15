(ns migration.add-in-params
  (:require [environ.core :as environ]
            [com.stuartsierra.component :as component]
            [malt-admin.helpers :refer [csv-to-list]]
            [clojurewerkz.cassaforte.query :refer :all]
            [malt-admin.system :as sys]
            [clojurewerkz.cassaforte.cql :as cql])
  (:gen-class))

 (defn -main [& args]
   (let [storage (-> environ/env
                     sys/new-system
                     :storage
                     component/start)

         c (:conn storage)]
     (cql/create-table c "in_params"
                       (column-definitions {:model_id :int
                                            :params :blob
                                            :primary-key [:model_id :params]}))
     (component/stop storage)))