(ns malt-admin.storage.models
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns]]
            [clojure.tools.logging :as log]))

(defn write-model! [storage model]
  (let [{:keys [conn]} storage]
    (cql/insert conn "models" model)))

(defn replace-model! [storage model]
  (let [{:keys [conn]} storage]
    (cql/update conn "models"
                (set (dissoc model :id))
                (where [[= :id (:id model)]]))))

(defn get-models [storage]
  (let [{:keys [conn]} storage]
    (cql/select conn "models" (columns :id :name :file :file_name :in_sheet_name :out_sheet_name))))

(defn get-model [storage id]
  (let [{:keys [conn]} storage]
    (first (cql/select conn "models"
                       (columns :id :name :file_name :in_sheet_name :out_sheet_name)
                       (where [[= :id id]])))))
