(ns malt-admin.form.settings)

(def settings
  {:fields [{:name :profiling-malt-host :type :text :label "Profiling malt host (malt on which model profiling will run)"}
            {:name :profiling-malt-port :type :number}]
   :validations [[:required [:profiling-malt-host :profiling-malt-port]]]})
