(ns malt-admin.form.configuration)

(def config
  {:fields [{:name :session-ttl :type :text :datatype :int}
            {:name :cache-table :type :text}
            {:name :rest-port :type :text :datatype :int}
            {:name :hosts :type :text :label "Cassandra hosts"}
            {:name :username :type :text}
            {:name :password :type :text}]
   :validations [[:required [:session-ttl
                             :cache-table
                             :rest-port
                             :hosts
                             :username
                             :password]]]})
