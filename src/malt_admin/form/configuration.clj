(ns malt-admin.form.configuration)

(def config
  {:fields [{:name :session-ttl :type :text :datatype :int}
            {:name :cache-table :type :text}
            {:name :rest-port :type :text :datatype :int}
            {:name :hosts :type :text}
            {:name :username :type :text}
            {:name :password :type :text}
            {:name :malt-nodes :type :text}]
   :validations [[:required [:session-ttl :malt-nodes :cache-table :rest-port :hosts :username :password]]]})
