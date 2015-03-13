(ns malt-admin.storage.log
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]))


(defn read-log [{conn :conn} id ssid]
  (first (cql/select conn "calculation_log"
                     (columns :session_id :model_id :in_params :out_params)
                     (where [[= :session_id ssid] [= :model_id id]]))))
