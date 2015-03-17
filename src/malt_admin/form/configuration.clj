(ns malt-admin.form.configuration)

(def config
  {:fields [{:name :session-ttl :type :text :datatype :int :label "Session TTL (1..60 minutes)"}
            {:name :cache-on :type :checkbox :datatype :boolean :value true :label "Turn on results caching?"}
            {:name :rest-port :type :text :datatype :int :label "REST port (on which malt will listen requests)"}
            {:name :hosts :type :text :label "Cassandra cluster hosts"}]
   :validations [[:min-val 1 :session-ttl]
                 [:max-val 60 :session-ttl]
                 [:required [:session-ttl
                             :rest-port
                             :hosts]]]})
