(ns malt-admin.form.model
  (:require [formative.core :as f]))

(def form
  {:fields      [{:name :id :type :text :datatype :int}
                 {:name :file :type :file}]
   :validations [[:required [:id]]]
   :enctype     "multipart/form-data"})

(def upload-form
  (-> form
      (update-in [:validations] conj [:required [:file]])))

(def edit-form
  (-> form
      (f/merge-fields [{:name :id :readonly true}])))
