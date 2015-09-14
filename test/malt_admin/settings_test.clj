(ns malt-admin.settings-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text value accept wait-until exists?]]
            [malt-admin.config :as c])
  (:import (org.openqa.selenium.support.ui ExpectedConditions)))

(deftest models-test
  (t/with-system [s (test-system @c/config)]
    (let [b (t/start-browser! s)]
      (signin b)
      (go b "/settings")
      (is (= 3 (count (elements b [:.form-shell :.fieldset-profiling-malt-host :input]))) "Should have 3 input fields")

      (testing "Successul update"
        (fill-in b "Profiling math engine host" "127.0.0.1")
        (fill-in b "Profiling math engine port" "6666")

        (click b "Update")
        (t/wait-condition b (ExpectedConditions/alertIsPresent))
        (accept b)

        (is (= "127.0.0.1" (value b "Profiling malt host")))
        (is (= "6666" (value b "Profiling malt port"))))


      (testing "Required filds validation"
        (fill-in b "Profiling math engine host" "")
        (fill-in b "Profiling math engine port" "")

        (click b "Update")
        (t/wait-condition b (ExpectedConditions/alertIsPresent))
        (accept b)

        (wait-until b (fn [b] (exists? b [:.form-problems :.control-label])) 5000 0)
        (is (= "Profiling math engine host, Profiling math engine port: must not be blank"
               (text b  :.control-label)))))))
