(ns malt-admin.controller.users
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [malt-admin.form.user :as form]
            [malt-admin.helpers :refer [redirect-with-flash]]
            [malt-admin.storage.users :as storage]
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
      (redirect-with-flash "/users" {:success (format "User \"%s\" successfully created" (:name values))}))))

(defn index [{{storage :storage} :web
              :as req}]
  (let [users (storage/get-users storage)]
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

(defn update [{params :params
               {storage :storage} :web
               :as req}]
  (fp/with-fallback #(malt-admin.controller.users/edit (assoc req :problems %))
    (let [values (fp/parse-params form/edit-form params)]
      (storage/update-user! storage (:login params) values)
      (redirect-with-flash "/users" {:success (format "User \"%s\" successfully updated" (:name values))}))))