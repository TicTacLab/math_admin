(ns malt-admin.form.user
  (:require [formative.core :as f]))

(def new-form
  {:fields      [{:name :name :type :text}
                 {:name :login :type :text}
                 {:name :password :type :password}
                 {:name :password_confirmation :type :password}]
   :validations [[:required [:name :login :password :password_confirmation]]
                 [:equal [:password :password_confirmation]]]})


(def edit-form
  {:fields      [{:name :name :type :text}]
   :validations [[:required [:name]]]})

(def edit-password-form
  {:fields      [{:name :password :type :password}
                 {:name :password_confirmation :type :password}]
   :validations [[:required [:password :password_confirmation]]
                 [:equal [:password :password_confirmation]]]})