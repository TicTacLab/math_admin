(ns malt-admin.models-test
  (:use clojure.test
        clj-webdriver.taxi)
  (:require [malt-admin.test-helper :as t :refer [test-system]]
            [environ.core :as environ]
            [com.stuartsierra.component :as component]))

(deftest models-test

 (let [system (component/start (test-system environ/env))
       b (t/start-browser! system)]
   
   (try
     (t/signin b)
     (t/go b "/models")
     (is (empty? (elements b :.model)))
     
     (click b "Upload New")
     (t/fill-in b "ID" "1")
     (t/fill-in b "Name" "SuperName")
     (send-keys b "File" *file*)
     (click b "Submit")
     (is (= "SuperName" (text b :.model-name)))
     
     (click b "Replace")
     (t/fill-in b "In sheet name" "MEGASHIT")
     (t/fill-in b "Out sheet name" "MEGASHUT")
     (send-keys b "File" "/etc/hosts")
     
     (click b "Submit")
     (is (= "MEGASHIT" (text b :.model-in-sheet-name)))
     (is (= "MEGASHUT" (text b :.model-out-sheet-name)))
     (is (= "hosts" (text b :.model-file-name)))
     
     (click b "Download")
     
     (click b "Delete")
     (is (empty? (elements b :.model)))

     (finally
       (component/stop system)))))
