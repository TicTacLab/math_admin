(ns malt-admin.form.configuration)

(def config
  {:fields [{:name :session-ttl :type :text :datatype :int}
            {:name :cache-table :type :text}
            {:name :rest-port :type :text :datatype :int}
            {:name :hosts :type :text :label "Cassandra hosts"}
            {:name :username :type :text}
            {:name :password :type :text}
            {:name :malt-nodes :type :text}
            {:name :malt-reload-model-url :type :text}
            {:name :calculation-malt-node :type :text}
            {:name :calculation-malt-port :type :text}]
   :validations [[:required [:session-ttl :calculation-malt-node :calculation-malt-port :malt-nodes :malt-reload-model-url :cache-table :rest-port :hosts :username :password]]]})
