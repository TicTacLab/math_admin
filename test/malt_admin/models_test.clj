(ns malt-admin.models-test
  (:use clojure.test
        kerodon.core
        kerodon.test)
  (:require [malt-admin.test-helper :as t :refer [test-system]]
            [environ.core :as environ]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]))

(deftest models-test
 (let [{{handler :handler} :web :as system} (component/start (test-system environ/env))]
  (try
   (-> (session handler)
       (t/signin)
       (visit "/models")
       (has (missing? [:.model]) "Should have no models")

       (follow "Upload New")
       
       (fill-in "ID" 1)
       (fill-in "Name" "Test Model")
       (attach-file "File" (io/file *file*))
       (press "Submit")
       (follow-redirect)

       (has (t/element? [:.model]) "Should have one model")
       (within [:.model :.model-name]
         (has (text? "Test Model")))
       
       (within [:.model]
         (follow "Replace"))
       
       (fill-in "In sheet name" "MEGASHIT")
       (attach-file "File" (io/file *file*))
       (press "Submit")
       (follow-redirect)

       (within [:.model :.model-in-sheet-name]
         (has (text? "MEGASHIT")))
       
       (within [:.model]
         (t/download-file "Download"))
       
       (has (status? 200) "Should return file")
       
       (visit "/models")
       (press "Delete")
       (follow-redirect)
       (has (missing? [:.model])))

   (finally
     (component/stop system)))))
