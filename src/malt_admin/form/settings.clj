(ns malt-admin.form.settings)

(def settings
  {:fields [{:name :malt-nodes :type :text}
            {:name :profiling-malt-host :type :text}
            {:name :profiling-malt-port :type :number}]
   :validations [[:required [:profiling-malt-host :profiling-malt-port :malt-nodes]]]})
