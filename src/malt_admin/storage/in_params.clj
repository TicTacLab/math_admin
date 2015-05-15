(ns malt-admin.storage.in-params
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]))

(defn delete! [{conn :conn} model-id]
  (cql/delete conn "in_params"
              (where [[= :model_id model-id]])))