(ns malt-admin.storage.in-params
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]))

(defn delete! [{conn :conn} model-id]
  (cql/delete conn "in_params"
              (where [[= :model_id model-id]])))

(defn get-in-params [{conn :conn} model-id]
  (cql/select conn "in_params"
              (columns :model_id :params)
              (where [[= :model_id model-id]])))