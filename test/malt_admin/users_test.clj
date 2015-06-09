(ns malt-admin.users-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system within signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text]]
            [malt-admin.config :as c]))

(deftest users-test
  (t/with-system [s (test-system @c/config)]
    (let [b (t/start-browser! s)
          login (str "super-duper-" (rand-int 10000))
          user-selector (keyword (format "tr[data-login='%s']" login))]
      (testing "Create"
        (signin b "admin" "admin1488")
        (go b "/users")
        (click b "Create")
        (fill-in b "Name" "Superman")
        (fill-in b "Login" login)
        (click b :#field-is_admin)
        (fill-in b "Password" "super-password")
        (fill-in b "Password confirmation" "super-password")
        (click b "Submit")

        (is (= (format "User \"%s\" successfully created" login)
               (text b :#flash-msg))
            "can't create user with login that already exists")
        (signout b))

      (testing "Rewrite existed user"
        (signin b)
        (go b "/users")
        (click b "Create")
        (fill-in b "Name" "Superman")
        (fill-in b "Login" login)
        (click b :#field-is_admin)
        (fill-in b "Password" "super-password-fail-password")
        (fill-in b "Password confirmation" "super-password-fail-password")
        (click b "Submit")

        (is (= (format "Login already exists \"%s\"" login)
              (text b :#flash-msg))
            "can't create user with login that already exists")
        (signout b))

      (testing "Signin newly created user"
        (signin b login "super-password")
        (is (= "You successfully signed in" (text :#flash-msg)) "User should be allowed to singin")
        (signout b))

      (testing "Edit"
        (signin b)
        (go b "/users")
        (within b user-selector
          (click "Edit"))
        (fill-in b "Name" "God")
        (click "Submit")
        (within b user-selector
          (is (= "God" (text :.user-name))
              "Should change user name"))
        (signout b))

      (testing "Password change"
        (signin b)
        (go b "/users")
        (within b user-selector
          (click "Password"))
        (fill-in b "Password" "simple-password")
        (fill-in b "Password confirmation" "simple-password")
        (click b "Submit")
        (signout b)
        (signin b login "simple-password")
        (is (= "You successfully signed in" (text b :#flash-msg)) "User should be allowed to singin")
        (go b "/users")
        (within b user-selector
          (click "Password"))
        (fill-in b "Password" "super-password")
        (fill-in b "Password confirmation" "super-password")
        (click b "Submit")
        (signout b))

      (testing "Activation/Deactivation"
        (signin b)
        (go b "/users")
        (within b user-selector
          (click "Deactivate"))
        (within b user-selector
          (is (= "inactive" (text :.user-status))
              "User status should be changed to inactive"))
        (signout b)

        (signin b login "simple-password")
        (is (re-find #"Invalid login or password"
                     (text b :.form-problems))
            "Inactive user should not be able to singin")

        (signin b)
        (go b "/users")
        (click b "Activate")
        (signout b)

        (signin b login "super-password")
        (is (= "You successfully signed in" (text b :#flash-msg)) "Active user should be allowed to singin")))))
