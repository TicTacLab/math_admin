(ns malt-admin.form.model
  (:require [formative.core :as f]))

(def form
  {:fields      [{:name :id :type :text :datatype :int}
                 {:name :name :type :text}
                 {:name :file :type :file}
                 {:name :in_sheet_name :type :text}
                 {:name :out_sheet_name :type :text}]
   :validations [[:required [:id :name :in_sheet_name :out_sheet_name]]]
   :enctype     "multipart/form-data"})

(def upload-form
  (-> form
      (update-in [:validations] conj [:required [:file]])))

(def edit-form
  (-> form
      (f/merge-fields [{:name :id :readonly true}
                       {:name :file_name :type :text :disabled true}])))
