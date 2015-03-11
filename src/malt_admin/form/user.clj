(ns malt-admin.form.user)

(def new-form
  {:fields      [{:name :name :type :text}
                 {:name :login :type :text}
                 {:name :is_admin :type :checkbox :label "Is Admin?" :datatype :boolean :value true}
                 {:name :password :type :password}
                 {:name :password_confirmation :type :password}]
   :validations [[:required [:name :login :password :password_confirmation]]
                 [:equal [:password :password_confirmation]]]})


(def edit-form
  {:fields      [{:name :name :type :text}
                 {:name :is_admin :type :checkbox :label "Is Admin?" :datatype :boolean :value true}]
   :validations [[:required [:name]]]})

(def edit-password-form
  {:fields      [{:name :password :type :password}
                 {:name :password_confirmation :type :password}]
   :validations [[:required [:password :password_confirmation]]
                 [:equal [:password :password_confirmation]]]})
