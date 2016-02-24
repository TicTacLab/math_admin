(ns malt-admin.storage.users
  (:require [malt-admin.storage :refer [sql-exception-handler]]
            [yesql.core :refer [defqueries]]
            [dire.core :refer [with-handler!]])
  (:import (java.sql SQLException)))

(defqueries "sql/users.sql")

(defn write-user! [{spec :pg-spec} user]
  (let [defaults {:status "active" :is_admin false}]
    (write-user*! (merge defaults user) {:connection spec})))

(with-handler! #'write-user!
  SQLException
  sql-exception-handler)

(defn get-users [{spec :pg-spec}]
  (get-users* {} {:connection spec}))

(with-handler! #'get-users
  SQLException
  sql-exception-handler)

(defn get-user [{spec :pg-spec} login]
  (first (get-user* {:login login} {:connection spec})))

(with-handler! #'get-user
  SQLException
  sql-exception-handler)

(defn get-user-with-password [{spec :pg-spec} login]
  (first (get-user-with-password* {:login login} {:connection spec})))

(with-handler! #'get-user-with-password
  SQLException
  sql-exception-handler)

(defn update-user! [{spec :pg-spec} login user]
  (let [params (assoc user :login login)]
    (cond
      (:password user) (update-user-password*! params {:connection spec})
      (:status user)   (update-user-status*! params {:connection spec})
      :else            (update-user-info*! params {:connection spec}))))

(with-handler! #'update-user!
  SQLException
  sql-exception-handler)