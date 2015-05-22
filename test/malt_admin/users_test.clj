(ns malt-admin.users-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text]]
            [malt-admin.config :as c]))

(deftest users-test
  (t/with-system [s (test-system @c/config)]
    (let [b (t/start-browser! s)]
      (testing "Create"
        (signin b)
        (go b "/users")
        (click b "Create")
        (fill-in b "Name" "Superman")
        (fill-in b "Login" "super-duper")
        (click b :#field-is_admin)
        (fill-in b "Password" "super-password")
        (fill-in b "Password confirmation" "super-password")
        (click b "Submit")

        (is (= 3 (count (elements b :.user))) "Should be only three users")
        (signout b))

      (testing "Rewrite existed user"
        (signin b)
        (go b "/users")
        (click b "Create")
        (fill-in b "Name" "Superman")
        (fill-in b "Login" "super-duper")
        (click b :#field-is_admin)
        (fill-in b "Password" "super-password-fail-password")
        (fill-in b "Password confirmation" "super-password-fail-password")
        (click b "Submit")

        (is (= "Login already exists \"super-duper\"" (text b :#flash-msg)) "can't create user with login that already exists")
        (signout b))

      (testing "Signin newly created user"
        (signin b "super-duper" "super-password")
        (is (= "You successfully signed in" (text b :#flash-msg)) "User should be allowed to singin")
        (signout b))

      (testing "Edit"
        (signin b)
        (go b "/users")
        (click (second (elements b "Edit")))
        (fill-in b "Name" "God")
        (click "Submit")
        (is (= "God" (text b (second (elements b :.user-name)))) "Should change user name")
        (signout b))

      (testing "Password change"
        (signin b)
        (go b "/users")
        (click (second (elements b "Password")))
        (fill-in b "Password" "simple-password")
        (fill-in b "Password confirmation" "simple-password")
        (click b "Submit")
        (signout b)
        (signin b "super-duper" "simple-password")
        (is (= "You successfully signed in" (text b :#flash-msg)) "User should be allowed to singin")
        (go b "/users")
        (click (second (elements b "Password")))
        (fill-in b "Password" "super-password")
        (fill-in b "Password confirmation" "super-password")
        (click b "Submit")
        (signout b))

      (testing "Activation/Deactivation"
        (signin b)
        (go b "/users")
        (click (second (elements b "Deactivate")))
        (is (= "inactive" (text b (nth (elements b :.user-status) 2))) "User status should be changed to inactive")
        (signout b)

        (signin b "super-duper" "simple-password")
        (is (re-find #"Invalid login or password"
                     (text b :.form-problems))
            "Inactive user should not be able to singin")

        (signin b)
        (go b "/users")
        (click b "Activate")
        (signout b)

        (signin b "super-duper" "super-password")
        (is (= "You successfully signed in" (text b :#flash-msg)) "Active user should be allowed to singin")))))
