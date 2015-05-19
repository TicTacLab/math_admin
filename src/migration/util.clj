(ns migration.util
  (:require [com.stuartsierra.component :as component]))

(defmacro with-component [[sym component] & body]
  `(try
     (let [~sym (component/start ~component)]
       (try
         (.. Runtime
             (getRuntime)
             (addShutdownHook (Thread. #(component/stop ~sym))))

         ~@body

         (System/exit 0)
         (catch Exception e#
           (.printStackTrace e#)
           (System/exit 1))
         (finally
           (component/stop ~sym))))
     (catch Exception e#
       (.printStackTrace e#)
       (System/exit 2))))