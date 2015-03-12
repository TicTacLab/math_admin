(ns malt-admin.persmissions-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [click send-keys text find-elements]]
            [environ.core :as environ]))

(deftest models-test
  (t/with-system [s (test-system environ/env)]
    (let [b (t/start-browser! s)]

      (testing "Model preloading"
        (signin b)
        (go b "/models")
        (is (empty? (elements b :.model)))
        (click b "Upload New")
        (fill-in b "ID" "1")
        (fill-in b "Name" "SuperName")
        (send-keys b "File" *file*)
        (click b "Submit")
        (is (= "SuperName" (text b :.model-name)))
        (signout b))

      (testing "Admin permissions"
        (signin b)

        (go b "/models")
        (is (seq (find-elements b {:xpath "//h1[text()='Models']"}))
            "Should have access to models")
        (is (seq (find-elements b {:xpath "//a[text()='Profile']"}))
            "Should see Profile btn")
        (is (seq (find-elements b {:xpath "//a[text()='Download']"}))
            "Should see Download btn")
        (is (seq (find-elements b {:xpath "//a[text()='Replace']"}))
            "Should see Replace btn")
        (is (seq (find-elements b {:xpath "//button[text()='Delete']"}))
            "Should see Delete btn")


        (go b "/users")
        (is (seq (find-elements b {:xpath "//h1[text()='Users']"}))
            "Should have access to users")

        (go b "/settings")
        (is (seq (find-elements b {:xpath "//h1[text()='Admin settings']"}))
            "Should have access to settings")

        (go b "/configuration")
        (is (seq (find-elements b {:xpath "//h1[text()='Configuration']"}))
            "Should have access to configuration")

        (signout b))


      (testing "User permissions"
        (signin b "guest" "guest")

        (go b "/models")
        (is (seq (find-elements b {:xpath "//h1[text()='Models']"}))
            "Should have access to models")
        (is (seq (find-elements b {:xpath "//a[text()='Profile']"}))
            "Should see Profile btn")
        (is (empty? (find-elements b {:xpath "//a[text()='Download']"}))
            "Should not see Download btn")
        (is (empty? (find-elements b {:xpath "//a[text()='Replace']"}))
            "Should not see Replace btn")
        (is (empty? (find-elements b {:xpath "//button[text()='Delete']"}))
            "Should not see Delete btn")


        (go b "/users")
        (is (seq (find-elements b {:xpath "//h1[text()='Forbidden']"}))
            "Should not have access to users")

        (go b "/settings")
        (is (seq (find-elements b {:xpath "//h1[text()='Forbidden']"}))
            "Should not have access to settings")

        (go b "/configuration")
        (is (seq (find-elements b {:xpath "//h1[text()='Forbidden']"}))
            "Should not have access to configuration")

        (go b "/models/1/download")
        (is (seq (find-elements b {:xpath "//h1[text()='Forbidden']"}))
            "Downloading should be strictly forbidden!")

        (go b "/models") ;; for signout button
        (signout b)))))
