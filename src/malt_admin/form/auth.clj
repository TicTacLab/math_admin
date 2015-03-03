(ns malt-admin.form.auth)

(def signin
  {:fields [{:name :login :type :text}
            {:name :password :type :password}]
   :validations [[:required [:login :password]]]})
