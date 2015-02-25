(ns malt-admin.form.model)

(def upload-form
  {:fields [{:name :id :type :text :datatype :int}
            {:name :name :type :text}
            {:name :file :type :file}
            {:name :in-sheet-name :type :text}
            {:name :out-sheet-name :type :text}]
   :validations [[:required [:id :name :file :in-sheet-name :out-sheet-name]]]
   :enctype "multipart/form-data"})
