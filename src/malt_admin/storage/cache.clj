(ns malt-admin.storage.cache
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]))

(defn clear [{conn :conn} model-id rev]
  (cql/delete conn "caches"
              (where [[= :model_id model-id]
                      [= :rev rev]])))