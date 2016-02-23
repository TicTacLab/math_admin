(ns malt-admin.storage.models
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns order-by]]
            [yesql.core :refer [defqueries]]
            [clojure.tools.logging :as log])
  (:import (javax.xml.bind DatatypeConverter)))

(defqueries "sql/files.sql")

(defn write-file! [{spec :pg-spec} file]
  (println file)
  (try
    (-> file
        (update :file #(DatatypeConverter/printBase64Binary %))
        (write-file*! {:connection spec}))
    (catch Exception e
      (log/error e "Exception occured during file writing into db"))))

(defn replace-model! [storage model]
  (let [{:keys [conn]} storage]
    (if-let [fields (seq (dissoc model :id))]
      (cql/update conn "models"
                  (set fields)
                  (where [[= :id (:id model)]])))))

(defn delete-model! [storage model-id]
  (let [{:keys [conn]} storage]
    (cql/delete conn "models"
                (where [[= :id model-id]]))))

(defn get-models [storage]
  (let [{:keys [conn]} storage]
    (sort-by :id (cql/select conn
                             "models"
                             (columns :id :name :file :file_name :in_sheet_name :out_sheet_name :last_modified :rev :content_type)))))

(defn get-model [storage id]
  (let [{:keys [conn]} storage]
    (cql/get-one conn "models"
                 (columns :id :name :file_name :in_sheet_name :out_sheet_name)
                 (where [[= :id id]]))))

(defn get-rev [storage id]
  (let [{:keys [conn]} storage]
    (:rev (cql/get-one conn "models"
                  (columns :rev)
                  (where [[= :id id]])))))

(defn get-model-file [storage id]
  (let [{:keys [conn]} storage]
    (cql/get-one conn "models"
                 (columns :id :name :file :file_name :in_sheet_name :out_sheet_name :last_modified :rev :content_type)
                 (where [[= :id id]]))))

(defn model-exists? [storage id]
  (let [{:keys [conn]} storage]
    (boolean (cql/get-one conn "models"
                          (columns :id)
                          (where [[= :id id]])))))
