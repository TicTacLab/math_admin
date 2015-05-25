(ns malt-admin.permissions-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in within]]
            [clj-webdriver.taxi :as w :refer [click send-keys text find-elements elements]]
            [malt-admin.config :as c]))

(deftest permissions-test
  (t/with-system [s (test-system @c/config)]
    (let [b (t/start-browser! s)]

      (testing "Model preloading"
        (let [id (rand-int 1000)]
          (signin)
          (go "/models")
          (click "Upload New")
          (fill-in "ID" (str id))
          (fill-in "Name" "SuperName")
          (send-keys "File" *file*)
          (click "Submit")
          (within b (keyword (format ".model[data-id='%d']" id))
            (is (= "SuperName" (text :.model-name))))
          (signout)))

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
        (is (seq (find-elements b {:xpath "//span[text()='Access denied']"}))
            "Should not have access to users")

        (go b "/settings")
        (is (seq (find-elements b {:xpath "//span[text()='Access denied']"}))
            "Should not have access to settings")

        (go b "/models/1/download")
        (is (seq (find-elements b {:xpath "//span[text()='Access denied']"}))
            "Downloading should be strictly forbidden!")

        (go b "/models") ;; for signout button
        (signout b)))))
