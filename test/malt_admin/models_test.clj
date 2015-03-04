(ns malt-admin.models-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text accept implicit-wait]]
            [environ.core :as environ]
            [clj-webdriver.taxi :as w]))

(deftest models-test
  (t/with-system [s (test-system environ/env)]
    (let [b (t/start-browser! s)]
      (signin b)
      (go b "/models")
      (is (empty? (elements b :.model)))

      (testing "Model uploading"
        (click b "Upload New")
        (fill-in b "ID" "1")
        (fill-in b "Name" "SuperName")
        (send-keys b "File" *file*)
        (click b "Submit")
        (is (= "SuperName" (text b :.model-name))))

      (testing "Replace"
        (click b "Replace")
        (fill-in b "In sheet name" "MEGASHIT")
        (fill-in b "Out sheet name" "MEGASHUT")
        (send-keys b "File" "/etc/hosts")
        (click b "Submit")
        (accept b)
        (implicit-wait b 100)
        (is (= "MEGASHIT" (text b :.model-in-sheet-name)))
        (is (= "MEGASHUT" (text b :.model-out-sheet-name)))
        (is (= "hosts" (text b :.model-file-name))))

      (testing "Download"
        (click b "Download"))

      (testing "Delete"
        (click b "Delete")
        (accept b)
        (implicit-wait b 100)
        (is (empty? (elements b :.model)))))))
