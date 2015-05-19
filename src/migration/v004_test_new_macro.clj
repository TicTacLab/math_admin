(ns migration.v004-test-new-macro
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
      (prn (cql/select c "models"
                   (q/columns :id :rev))))))
