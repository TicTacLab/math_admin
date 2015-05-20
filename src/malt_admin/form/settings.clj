(ns malt-admin.form.settings)

(def settings
  {:fields [{:name :profiling-malt-host :type :text :label "Profiling malt host" :note "malt on which model profiling will run"}
            {:name :profiling-malt-port :type :number}
            {:name :cache-filler-addr :type :text :label "Cache filler address" :note "malt engine addr (X.X.X.X:XXXX) for execute filler requests"}]
   :validations [[:required [:profiling-malt-host :profiling-malt-port :cache-filler-addr]]]}
  )
