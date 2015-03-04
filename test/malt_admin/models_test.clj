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
     (input-text b :#field-id "1")
     (input-text b :#field-name "SuperName")
     (send-keys b :#field-file *file*)
     (click b "Submit")
     (is (= "SuperName" (text b :.model-name)))
     
     (click b "Replace")
     (clear b :#field-in_sheet_name)
     (input-text b :#field-in_sheet_name "MEGASHIT")
     (clear b :#field-out_sheet_name)
     (input-text b :#field-out_sheet_name "MEGASHUT")
     (send-keys b :#field-file "/etc/hosts")
     
     (click b "Submit")
     (is (= "MEGASHIT" (text b :.model-in-sheet-name)))
     (is (= "MEGASHUT" (text b :.model-out-sheet-name)))
     (is (= "hosts" (text b :.model-file-name)))
     
     (click b "Download")
     
     (click b "Delete")
     (is (empty? (elements b :.model)))

     (finally
       (component/stop system)))))
