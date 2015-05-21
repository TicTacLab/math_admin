(ns malt-admin.settings-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text value accept wait-until exists?]]
            [environ.core :as environ]))

(deftest models-test
  (t/with-system [s (test-system environ/env)]
    (let [b (t/start-browser! s)]
      (signin b)
      (go b "/settings")
      (is (= 3 (count (elements b [:.form-shell :.fieldset-profiling-malt-host :input]))) "Should have 3 input fields")

      (testing "Successul update"
        (fill-in b "Profiling malt host" "127.0.0.1")
        (fill-in b "Profiling malt port" "6666")
        (fill-in b "Cache filler address" "127.0.0.1:6666")

        (click b "Update")
        (accept b)

        (is (= "127.0.0.1" (value b "Profiling malt host")))
        (is (= "6666" (value b "Profiling malt port")))
        (is (= "127.0.0.1:6666" (value b "Cache filler address"))))


      (testing "Required filds validation"
        (fill-in b "Profiling malt host" "")
        (fill-in b "Profiling malt port" "")
        (fill-in b "Cache filler address" "")

        (click b "Update")
        (accept b)

        (wait-until b (fn [b] (exists? b [:.form-problems :.control-label])) 5000 0)
        (is (= "Profiling malt host, Profiling malt port, Cache filler address: must not be blank"
               (text b  :.control-label)))))))
