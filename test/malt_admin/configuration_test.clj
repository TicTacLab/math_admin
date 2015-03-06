(ns malt-admin.configuration-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text value accept wait-until exists?]]
            [environ.core :as environ]))

(deftest models-test
  (t/with-system [s (test-system environ/env)]
    (let [b (t/start-browser! s)]
      (signin b)
      (go b "/configuration")
      (is (= 6 (count (elements b [:.form-shell :.fieldset-session-ttl :input]))) "Should have 6 input fields")

      (testing "Successul update"
        (fill-in b "Session TTL (min)" "60")
        (fill-in b "Cache table" "crap.workbook")
        (fill-in b "Rest port" "6666")
        (fill-in b "Cassandra hosts" "127.0.0.1")
        (fill-in b "Username" "jack")
        (fill-in b "Password" "daniel's")
        (click b "Update")
        (accept b)

        (is (= "60" (value b "Session TTL (min)")))
        (is (= "crap.workbook" (value b "Cache table")))
        (is (= "6666" (value b "Rest port")))
        (is (= "127.0.0.1" (value b "Cassandra hosts")))
        (is (= "jack" (value b "Username")))
        (is (= "daniel's" (value b "Password"))))

      (testing "Failing TTL validation"
        (fill-in b "Session TTL (min)" "40")
        (click b "Update")
        (accept b)

        (fill-in b "Session TTL (min)" "100")
        (click b "Update")
        (accept b)
        (wait-until b (fn [b] (exists? b [:.form-problems :.control-label])) 5000 0)
        (is (= "Session TTL (min): cannot be more than 60" (text b  :.control-label)))))))
