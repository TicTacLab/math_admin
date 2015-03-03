(ns malt-admin.form.settings)

(def settings
  {:fields [{:name :malt-nodes :type :text}
            {:name :calculation-malt-node :type :text :label "Profiling malt host"}
            {:name :calculation-malt-port :type :number :label "Profiling malt port"}]
   :validations [[:required [:calculation-malt-node :calculation-malt-port :malt-nodes]]]})
