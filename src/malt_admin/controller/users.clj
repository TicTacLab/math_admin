(ns malt-admin.controller.users
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [malt-admin.form.user :as form]
            [malt-admin.helpers :refer [redirect-with-flash]]
            [malt-admin.storage.users :as storage]
            [malt-admin.audit :refer [audit]]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [clojurewerkz.scrypt.core :as sc]
            [formative.core :as f]
            [org.httpkit.client :as http]
            [clojure.tools.logging :as log]))


(defn new* [{:keys [problems params] :as req}]
  (render "users/new" req {:new-form (assoc form/new-form
                                       :problems problems
                                       :values params
                                       :action "/users"
                                       :method "POST")}))

(defn ^:private encrypt-password [params]
  (update-in params [:password] sc/encrypt 16384 8 1))

(defn create [{params :params
               {storage :storage} :web
               :as req}]
  (fp/with-fallback #(malt-admin.controller.users/new* (assoc req :problems %))
    (let [values (fp/parse-params form/new-form params)]
      (storage/write-user! storage (-> values
                                       (encrypt-password)
                                       (dissoc :password_confirmation)))
      (audit req :create-user (dissoc values :password_confirmation :password))
      (redirect-with-flash "/users" {:success (format "User \"%s\" successfully created" (:name values))}))))

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
                                           :action (format "/users/%s" login)
                                           :method "PUT")
                              :user      user})))

(defn edit-password [{{login :login :as params} :params
                      problems                  :problems
                      :as                       req}]
  (render "users/edit-password" req {:edit-password-form (assoc form/edit-password-form
                                                           :values params
                                                           :problems problems
                                                           :action (format "/users/%s/update-password" login)
                                                           :method "PUT")
                                     :user               {:login login}}))

(defn update [{params :params
               {storage :storage} :web
               :as req}]
  (fp/with-fallback #(malt-admin.controller.users/edit (assoc req :problems %))
    (let [values (fp/parse-params form/edit-form params)]
      (storage/update-user! storage (:login params) values)
      (audit req :update-user (select-keys params [:login :name]))
      (redirect-with-flash "/users" {:success (format "User \"%s\" successfully updated" (:name values))}))))

(defn update-password [{params :params
                        {storage :storage} :web
                        :as req}]
  (fp/with-fallback #(malt-admin.controller.users/edit-password (assoc req :problems %))
    (let [values (fp/parse-params form/edit-password-form params)]
      (storage/update-user! storage (:login params) (-> values
                                                        (encrypt-password)
                                                        (dissoc :password_confirmation)))
      (audit req :update-user-password (select-keys params [:login]))
      (redirect-with-flash "/users" {:success (format "Password for \"%s\" successfully updated" (:name values))}))))

(defn change-status [{{:keys [action login]} :params
                      {storage :storage}     :web :as req}]
  (if-let [status (case action
                    "activate" "active"
                    "deactivate" "inactive"
                    nil)]
    (do
      (storage/update-user! storage login {:status status})
      (audit req :change-user-status {:login login :status status})
      (redirect-with-flash "/users" {:success (format "Change status for user \"%s\" to \"%s\"" login status)}))
    (redirect-with-flash "/users" {:error (format "Bad action \"%s\"" action)})))
