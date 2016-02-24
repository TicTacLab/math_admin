(ns malt-admin.storage.models
  (:require [yesql.core :refer [defqueries]]
            [dire.core :refer [with-handler!]]
            [clojure.tools.logging :as log])
  (:import (javax.xml.bind DatatypeConverter)
           (java.sql Timestamp SQLException)))

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

(defn base64-decode [m ks]
  (reduce (fn [acc k]
            (update acc k #(DatatypeConverter/parseBase64Binary %)))
          m ks))

(defqueries "sql/files.sql")

(def sql-exception-handler
  (fn [e & args]
    (log/error e "Exception occured during file writing into db")))

;; ========= Public API

(defn write-file! [{spec :pg-spec} file]
  (-> file
      (coerce-time [:last_modified])
      (base64-encode [:file])
      (write-file*! {:connection spec})))

(with-handler! #'write-file!
  SQLException
  sql-exception-handler)


(defn update-file! [{spec :pg-spec} file]
  (if-not (:file file)
    (update-without-raw-file*! file {:connection spec})
    (let [file (-> file
                   (coerce-time [:last_modified])
                   (base64-encode [:file]))]
      (update-with-raw-file*! file {:connection spec}))))

(with-handler! #'update-file!
  SQLException
  sql-exception-handler)


(defn delete-file! [{spec :pg-spec} id]
  (delete-file*! {:id id} {:connection spec}))

(with-handler! #'delete-file!
  SQLException
  sql-exception-handler)


(defn get-files [{spec :pg-spec}]
  (sort-by :id (get-files* {} {:connection spec})))

(with-handler! #'get-files
  SQLException
  sql-exception-handler)


(defn get-file [{spec :pg-spec} id]
  (first (get-file* {:id id} {:connection spec})))

(with-handler! #'get-file
  SQLException
  sql-exception-handler)


(defn get-rev [{spec :pg-spec} id]
  (:rev (get-rev* {:id id} {:connection spec})))

(with-handler! #'get-rev
  SQLException
  sql-exception-handler)


(defn get-raw-file [{spec :pg-spec} id]
  (-> (get-raw-file* {:id id} {:connection spec})
      first
      (base64-decode [:file])))

(with-handler! #'get-raw-file
  SQLException
  sql-exception-handler)


(defn file-exists? [{spec :pg-spec} id]
  (boolean (first (file-exists*? {:id id} {:connection spec}))))

(with-handler! #'file-exists?
  SQLException
  sql-exception-handler)