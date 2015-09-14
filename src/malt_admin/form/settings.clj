(ns malt-admin.form.settings)

(def settings
  {:fields [{:name :profiling-malt-host :type :text :label "Profiling math engine host" :note "math engine on which model profiling will run"}
            {:name :profiling-malt-port :type :number :label "Profiling math engine port"}]
   :validations [[:required [:profiling-malt-host :profiling-malt-port]]]}
  )
