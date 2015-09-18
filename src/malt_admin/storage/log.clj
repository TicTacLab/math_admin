(ns malt-admin.storage.log
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [cheshire.core :as json])
  (:import (com.datastax.driver.core.utils Bytes)))

(defn read-log [{conn :conn} id ssid]
  (some-> (cql/get-one conn "calculation_log"
                      (columns :session_id :model_id :in_params :out_params)
                      (where [[= :session_id ssid] [= :model_id id]]))
          (update-in [:out_params] #(json/parse-string  (String. (Bytes/getArray %))))))