(ns malt-admin.main
  (:require [malt-admin.system :as s]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [noilly.core :as noilly]
            [malt-admin.config :as c])
  (:gen-class))

(def system (atom nil))
(def noilly-srv (atom nil))

(defn -main [& _args]
  (try
    (swap! system #(if % % (component/start (s/new-system (c/config)))))
    (println "MathAdmin is started!")
    (catch Exception e
      (println e)
      (log/error e "Exception during startup. Fix configuration and
                    start application using REST configuration interface")))
  (swap! noilly-srv
         (fn [srv]
           (if srv
             srv
             (noilly/start c/cfg
                           #(swap! system
                                   (fn [s]
                                     (when s (component/stop s))
                                     (c/load-config)
                                     (component/start (s/new-system (c/config)))))))))
  (.. Runtime
      (getRuntime)
      (addShutdownHook (Thread. (fn []
                                  (do
                                    (component/stop @system)
                                    (noilly/stop @noilly-srv)))))))