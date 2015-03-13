(ns malt-admin.form.configuration)

(def config
  {:fields [{:name :session-ttl :type :text :datatype :int :label "Session TTL (min)"}
            {:name :cache-on :type :checkbox :datatype :boolean :value true}
            {:name :rest-port :type :text :datatype :int}
            {:name :hosts :type :text :label "Cassandra hosts"}]
   :validations [[:min-val 1 :session-ttl]
                 [:max-val 60 :session-ttl]
                 [:required [:session-ttl
                             :rest-port
                             :hosts]]]})
