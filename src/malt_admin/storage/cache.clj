(ns malt-admin.storage.cache
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]))

(defn clear [{conn :conn} model-id]
  (cql/delete conn "cache"
              (where [[= :model_id model-id]])))