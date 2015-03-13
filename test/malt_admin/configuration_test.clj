(ns malt-admin.configuration-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text value accept wait-until exists? select]]
            [environ.core :as environ]))

(deftest models-test
  (t/with-system [s (test-system environ/env)]
    (let [b (t/start-browser! s)]
      (signin b)
      (go b "/configuration")
      (is (= 4 (count (elements b [:.form-shell :.fieldset-session-ttl :input]))) "Should have specified number input fields")

      (testing "Successul update"
        (fill-in b "Session TTL (min)" "60")
        (select b "Cache on")
        (fill-in b "Rest port" "6666")
        (fill-in b "Cassandra hosts" "127.0.0.1")
        (click b "Update")
        (accept b)

        (is (= "60" (value b "Session TTL (min)")))
        (is (= "6666" (value b "Rest port")))
        (is (= "127.0.0.1" (value b "Cassandra hosts")))
        (is (= "true" (value b "Cache on"))))

      (testing "Failing TTL validation"
        (fill-in b "Session TTL (min)" "40")
        (click b "Update")
        (accept b)

        (fill-in b "Session TTL (min)" "100")
        (click b "Update")
        (accept b)
        (wait-until b (fn [b] (exists? b [:.form-problems :.control-label])) 5000 0)
        (is (= "Session TTL (min): cannot be more than 60" (text b  :.control-label)))))))
