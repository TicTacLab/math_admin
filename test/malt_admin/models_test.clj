(ns malt-admin.models-test
 (:use clojure.test
       kerodon.core
       kerodon.test)
 (:require [malt-admin.system :as sys]
           [environ.core :as environ]
           [com.stuartsierra.component :as component]
           [clojure.java.io :as io]
           [net.cgrand.enlive-html :as enlive]))

(defmacro element? [selector]
  `(validate >
             #(count (enlive/select (:enlive %) ~selector))
             0
             (~'element? ~selector)))

(deftest models-test
 (let [{{handler :handler} :web :as system} (component/start (sys/new-system environ/env))]
  (try
   (-> (session handler)
       (visit "/models")
       (has (missing? [:.model]) "Should have no models")
       
       (follow "Upload New")
       (fill-in "ID" 1)
       (fill-in "Name" "Test Model")
       (attach-file "File" (io/file *file*))
       (press "Submit")

       (has (element? [:.model]) "Should have one model")
       (within [:.model :.model-name]
         (has (text? "Test Model"))))

   (catch Exception e
    (component/stop system)
    (throw e)))))
