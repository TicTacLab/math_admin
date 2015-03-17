(ns malt-admin.storage.log
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [flatland.protobuf.core :as pb]
            [malt-admin.helpers :refer [Packet]])
  (:import (com.datastax.driver.core.utils Bytes)))


(defn read-log [{conn :conn} id ssid]
  (-> (cql/select conn "calculation_log"
                  (columns :session_id :model_id :in_params :out_params)
                  (where [[= :session_id ssid] [= :model_id id]]))
      (first)
      (update-in [:out_params] #(pb/protobuf-load Packet (Bytes/getArray %)))))
