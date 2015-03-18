(ns malt-admin.form.configuration)

(def config
  {:fields [{:name :session-ttl :type :text :datatype :int :label "Session TTL (min)" :note "1..60 minutes"}
            {:name :cache-on :type :checkbox :datatype :boolean :value true :label "Cache on" :note "if on malt will cache calculation results"}
            {:name :rest-port :type :text :datatype :int :label "Rest port" :note "port on which malt will accept requests"}
            {:name :hosts :type :text :label "Cassandra hosts"}]
   :validations [[:min-val 1 :session-ttl]
                 [:max-val 60 :session-ttl]
                 [:required [:session-ttl
                             :rest-port
                             :hosts]]]})
