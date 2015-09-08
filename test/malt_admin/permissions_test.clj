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
          (Thread/sleep 300)
          (within b (keyword (format ".model[data-id='%d']" id))
            (is (= "SuperName" (text :.model-name))))))

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

        (signout b)))))
