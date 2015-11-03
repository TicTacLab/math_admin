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
          (go "/mengine/files")
          (click "Upload New")
          (fill-in "ID" (str id))
          (send-keys "File" "/etc/hosts")
          (click "Submit")
          (Thread/sleep 300)
          (within b (keyword (format ".model[data-id='%d']" id))
            (is (= "hosts" (text :.model-file-name))))))

      (testing "Admin permissions"
        (signin b)

        (go b "/mengine/files")
        (is (seq (find-elements b {:xpath "//h1[text()='Files']"}))
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

        (signout b)))))
