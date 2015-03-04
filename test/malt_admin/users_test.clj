(ns malt-admin.users-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text]]
            [environ.core :as environ]))

(deftest models-test
  (t/with-system [s (test-system environ/env)]
    (let [b (t/start-browser! s)]
      (signin b)
      (go b "/users")
      (is (= 1 (count (elements b :.user))) "Should be only one user")

      (testing "Create"
        (click b "Create")
        (fill-in b "Name" "Superman")
        (fill-in b "Login" "super-duper")
        (fill-in b "Password" "super-password")
        (fill-in b "Password confirmation" "super-password")
        (click b "Submit")

        (is (= 2 (count (elements b :.user))) "Should be only two users"))

      (testing "Signin newly created user"
        (signout b)
        (signin b "super-duper" "super-password")
        (is (= "You successfully signed in" (text b :#flash)) "User should be allowed to singin")
        (signout b)
        (signin b))

      (testing "Edit"
        (click (second (elements b "Edit")))
        (fill-in b "Name" "God")
        (click "Submit")
        (is (= "God" (text b (second (elements b :.user-name)))) "Should change user name"))

      (testing "Password change"
        (click (second (elements b "Password")))
        (fill-in b "Password" "simple-password")
        (fill-in b "Password confirmation" "simple-password")
        (click b "Submit")
        (signout b)
        (signin b "super-duper" "simple-password")
        (is (= "You successfully signed in" (text b :#flash)) "User should be allowed to singin")
        (signout b)
        (signin b))
      
      (testing "Activation/Deactivation"
        (click (second (elements b "Deactivate")))
        (is (= "inactive" (text b (second (elements b :.user-status)))) "User status should be changed to inactive")
        (signin b "super-duper" "simple-password")
        (is (re-find #"Invalid login or password"
                     (text b :.form-problems))
            "Inactive user should not be able to singin")
        (signin b)
        (click b "Activate")
        (signout b)
        (signin b "super-duper" "simple-password")
        (is (= "You successfully signed in" (text b :#flash)) "Active user should be allowed to singin")))))
