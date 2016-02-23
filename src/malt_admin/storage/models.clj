(ns malt-admin.storage.models
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns order-by]]
            [yesql.core :refer [defqueries]]
            [clojure.tools.logging :as log])
  (:import (javax.xml.bind DatatypeConverter)
           (java.sql Timestamp)))

(defn to-time [date]
  (Timestamp. (.getTime date)))

(defn coerce-time [m ks]
  (reduce (fn [acc k]
            (update acc k to-time))
          m ks))

(defn base64-encode [m ks]
  (reduce (fn [acc k]
            (update acc k #(DatatypeConverter/printBase64Binary %)))
          m ks))

(defqueries "sql/files.sql")

(defn write-file! [{spec :pg-spec} file]
  (try
    (-> file
        (coerce-time [:last_modified])
        (base64-encode [:file])
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

(defn get-files [{spec :pg-spec}]
  (sort-by :id (get-files* {} {:connection spec})))

(defn get-file [{spec :pg-spec} id]
  (first (get-file* {:id id} {:connection spec})))

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
