(ns migration.create-caches-with-revision
  (:require [com.stuartsierra.component :as component]
            [malt-admin.system :as sys]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as q]
            [malt-admin.config :as c])
  (:gen-class))


(defn -main [& args]
  (let [storage (-> @c/config
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
                         :primary-key [[:model_id :rev] :params]}))))