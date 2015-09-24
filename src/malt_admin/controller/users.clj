(ns malt-admin.controller.users
  (:require [malt-admin.view :refer (render u)]
            [malt-admin.form.user :as form]
            [malt-admin.helpers :refer [redirect-with-flash]]
            [malt-admin.storage.users :as storage]
            [malt-admin.audit :as audit]
            [malt-admin.password :as pass]
            [formative.parse :as fp]
            [clojurewerkz.scrypt.core :as sc]
            [ring.util.response :as res]
            [cheshire.core :as json]
            [clojure.java.io :as io])
  (:import (java.io Reader)))


(defn new* [{:keys [problems params] :as req}]
  (render "users/new" req {:new-form (assoc form/new-form
                                       :problems problems
                                       :values params
                                       :action "/users"
                                       :method "POST")}))

(defn ^:private encrypt-password [params]
  (update-in params [:password] sc/encrypt 16384 8 1))

;; --------------------
;; Actions
;; --------------------

(defn create [{params :params
               {storage :storage} :web
               :as req}]
  (fp/with-fallback #(malt-admin.controller.users/new* (assoc req :problems %))
    (let [values (fp/parse-params form/new-form params)
          user-login (:login values)
          user (storage/get-user storage user-login)]
      (if user
        (redirect-with-flash "/users" {:error (format "Login already exists \"%s\"" user-login)})
        (do (storage/write-user! storage (-> values
                                             (encrypt-password)
                                             (dissoc :password_confirmation)))
            (audit/info req :create-user (dissoc values :password_confirmation :password))
            (redirect-with-flash "/users" {:success (format "User \"%s\" successfully created" (:login values))}))))))

(defn index [{{storage :storage} :web
              :as req}]
  (let [users (sort-by :status (storage/get-users storage))]
    (render "users/index" req {:users users})))

(defn edit [{{storage :storage}        :web
             {login :login :as params} :params
             problems                  :problems
             :as                       req}]
  (let [user (storage/get-user storage login)]
    (render "users/edit" req {:edit-form (assoc form/edit-form
                                           :values (if problems params user)
                                           :problems problems
                                           :action (format "/users/%s" (u login))
                                           :method "PUT")
                              :user      user})))

(defn edit-password [{{login :login :as params} :params
                      problems                  :problems
                      :as                       req}]
  (render "users/edit-password" req {:edit-password-form (assoc form/edit-password-form
                                                           :values params
                                                           :problems problems
                                                           :action (format "/users/%s/update-password" (u login))
                                                           :method "PUT")
                                     :user               {:login login}}))

(defn update [{params :params
               {storage :storage} :web
               :as req}]
  (fp/with-fallback #(malt-admin.controller.users/edit (assoc req :problems %))
    (let [values (merge {:is_admin false} (fp/parse-params form/edit-form params))]
      (storage/update-user! storage (:login params) values)
      (audit/info req :update-user (select-keys params [:login :name :is_admin]))
      (redirect-with-flash "/users" {:success (format "User \"%s\" successfully updated" (:login params))}))))

(defn update-password [{params :params
                        {storage :storage} :web
                        :as req}]
  (fp/with-fallback #(malt-admin.controller.users/edit-password (assoc req :problems %))
    (let [values (fp/parse-params form/edit-password-form params)]
      (storage/update-user! storage (:login params) (-> values
                                                        (encrypt-password)
                                                        (dissoc :password_confirmation)))
      (audit/info req :update-user-password (select-keys params [:login]))
      (redirect-with-flash "/users" {:success (format "Password for \"%s\" successfully updated" (:login params))}))))

(defn change-status [{{:keys [action login]} :params
                      {storage :storage}     :web :as req}]
  (if-let [status (case action
                    "activate" "active"
                    "deactivate" "inactive"
                    nil)]
    (do
      (storage/update-user! storage login {:status status})
      (audit/info req :change-user-status {:login login :status status})
      (redirect-with-flash "/users" {:success (format "Change status for user \"%s\" to \"%s\"" login status)}))
    (redirect-with-flash "/users" {:error (format "Bad action \"%s\"" action)})))

(defn pass-analyze [req]
  (let [pass (-> req :body .bytes slurp json/parse-string (get "password"))
        grade (pass/analyze pass)]
    (-> (format "{\"grade\": %d}" grade)
        (res/response)
        (res/content-type "application/json"))))