(ns malt-admin.form.model)

(def upload-form
  {:fields      [{:name :id :type :text :datatype :int}
                 {:name :name :type :text}
                 {:name :file :type :file}
                 {:name :in_sheet_name :type :text}
                 {:name :out_sheet_name :type :text}]
   :validations [[:required [:id :name :file :in_sheet_name :out_sheet_name]]]
   :enctype     "multipart/form-data"})
