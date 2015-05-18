(ns malt-admin.storage.models
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns order-by]])
  (:import (java.util Date)))



(defn write-model! [storage model]
  (let [{:keys [conn]} storage]
    (cql/insert conn "models" model)))

(defn replace-model! [storage model]
  (let [{:keys [conn]} storage]
    (cql/update conn "models"
                (set (dissoc model :id))
                (where [[= :id (:id model)]]))))

(defn delete-model! [storage model-id]
  (let [{:keys [conn]} storage]
    (cql/delete conn "models"
                (where [[= :id model-id]]))))

(defn get-models [storage]
  (let [{:keys [conn]} storage]
    (sort-by :id (cql/select conn "models"
                             (columns :id :name :file :file_name :in_sheet_name :out_sheet_name :last_modified)))))

(defn get-model [storage id]
  (let [{:keys [conn]} storage]
    (cql/get-one conn "models"
                 (columns :id :name :file_name :in_sheet_name :out_sheet_name)
                 (where [[= :id id]]))))

(defn get-model-file [storage id]
  (let [{:keys [conn]} storage]
    (cql/get-one conn "models"
                (columns :id :file_name :file :content_type)
                (where [[= :id id]]))))

(defn model-exists? [storage id]
  (let [{:keys [conn]} storage]
    (boolean (cql/get-one conn "models"
                          (columns :id)
                          (where [[= :id id]])))))
