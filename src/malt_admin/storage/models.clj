(ns malt-admin.storage.models
  (:require [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :refer [where columns order-by using]])
  (:import (java.util UUID)))

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

(defn write-draft-model! [storage draft session-id]
  (let [{:keys [conn]} storage
        {:keys [file file-name content-type]} draft]
    (cql/insert conn "draft_models"
                {:file         file
                 :file_name    file-name
                 :content_type content-type
                 :session_id   (UUID/fromString session-id)})))

(defn get-draft-model [storage ssid]
  (let [{:keys [conn]} storage]
    (cql/get-one conn "draft_models"
                 (columns :file :file_name :content_type)
                 (where [[= :session_id (UUID/fromString ssid)]]))))
