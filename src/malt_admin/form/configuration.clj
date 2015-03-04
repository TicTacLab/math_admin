(ns malt-admin.form.configuration)

(def config
  {:fields [{:name :session-ttl :type :text :datatype :int :label "Session TTL (min)"}
            {:name :cache-table :type :text}
            {:name :rest-port :type :text :datatype :int}
            {:name :hosts :type :text :label "Cassandra hosts"}
            {:name :username :type :text}
            {:name :password :type :text}]
   :validations [[:min-val 1 :session-ttl]
                 [:max-val 60 :session-ttl]
                 [:required [:session-ttl
                             :cache-table
                             :rest-port
                             :hosts
                             :username
                             :password]]]})
