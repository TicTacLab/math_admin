(ns migration.v005-add-cache-q-table
  (:require [migration.util :refer [with-component]]
            [environ.core :as environ]
            [malt-admin.system :as sys]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as q])
  (:gen-class))

(defn -main [& args]
  (with-component [storage (-> environ/env
                               sys/new-system
                               :storage)]
                  (let [c (:conn storage)]
                    (cql/create-table c "cache_q"
                                      (q/column-definitions
                                        {:model_id    :int
                                         :rev         :varchar
                                         :params      :blob
                                         :primary-key [[:model_id :rev] :params]}))
                    )))