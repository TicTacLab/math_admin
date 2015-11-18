(ns malt-admin.revision-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in wait within]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text accept implicit-wait]]
            [malt-admin.config :as c])
  (:import (org.openqa.selenium.support.ui ExpectedConditions)))

(deftest revision-test
  (t/with-system [s (test-system (c/config))]
    (let [b (t/start-browser! s)]
      (signin)
      (go "/mengine/files")

      (let [id (rand-int 1000000)
            model-selector (keyword (format ".model[data-id='%d']" id))]
        (testing "revision creating on model upload"
          (click "Upload New")
          (fill-in "ID" (str id))
          (send-keys "File" *file*)
          (click "Submit")
          (Thread/sleep 300)
          (is (seq (w/attribute model-selector "data-rev"))
              "should set rev on upload"))

        (testing "Replace with file"
          (let [old-rev (w/attribute model-selector "data-rev")]
            (within b model-selector
              (click "Replace"))
            (send-keys "File" "/etc/hosts")
            (click "Submit")
            (t/wait-condition b (ExpectedConditions/alertIsPresent))
            (accept)
            (wait 100)
            (is (not= old-rev
                      (w/attribute model-selector "data-rev"))
                "should change revision if file sent")))

        (testing "Replace without file"
          (let [old-rev (w/attribute model-selector "data-rev")]
            (within b model-selector
              (click "Replace"))
            (click "Submit")
            (t/wait-condition b (ExpectedConditions/alertIsPresent))
            (accept)
            (wait 100)
            (is (= old-rev
                      (w/attribute model-selector "data-rev"))
                "should not change revision if file not sent")))))))
