(ns malt-admin.models-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system]]
            [environ.core :as environ]
            [clj-webdriver.taxi :as w]))

(deftest models-test
  (t/with-system [s (test-system environ/env)]
    (let [b (t/start-browser! s)]
      (t/signin b)
      (t/go b "/models")
      (is (empty? (w/elements b :.model)))

      (w/click b "Upload New")
      (t/fill-in b "ID" "1")
      (t/fill-in b "Name" "SuperName")
      (w/send-keys b "File" *file*)
      (w/click b "Submit")
      (is (= "SuperName" (w/text b :.model-name)))

      (w/click b "Replace")
      (t/fill-in b "In sheet name" "MEGASHIT")
      (t/fill-in b "Out sheet name" "MEGASHUT")
      (w/send-keys b "File" "/etc/hosts")

      (w/click b "Submit")
      (is (= "MEGASHIT" (w/text b :.model-in-sheet-name)))
      (is (= "MEGASHUT" (w/text b :.model-out-sheet-name)))
      (is (= "hosts" (w/text b :.model-file-name)))

      (w/click b "Download")

      (w/click b "Delete")
      (is (empty? (w/elements b :.model))))))
