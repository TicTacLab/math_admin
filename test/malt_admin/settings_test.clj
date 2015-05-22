(ns malt-admin.settings-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text value accept wait-until exists?]]
            [malt-admin.system :as s]))

(deftest models-test
  (t/with-system [s (test-system @s/config)]
    (let [b (t/start-browser! s)]
      (signin b)
      (go b "/settings")
      (is (= 2 (count (elements b [:.form-shell :.fieldset-profiling-malt-host :input]))) "Should have 3 input fields")

      (testing "Successul update"
        (fill-in b "Profiling malt host" "127.0.0.1")
        (fill-in b "Profiling malt port" "6666")

        (click b "Update")
        (accept b)

        (is (= "127.0.0.1" (value b "Profiling malt host")))
        (is (= "6666" (value b "Profiling malt port"))))


      (testing "Required filds validation"
        (fill-in b "Profiling malt host" "")
        (fill-in b "Profiling malt port" "")

        (click b "Update")
        (accept b)

        (wait-until b (fn [b] (exists? b [:.form-problems :.control-label])) 5000 0)
        (is (= "Profiling malt host, Profiling malt port: must not be blank"
               (text b  :.control-label)))))))
