(ns malt-admin.models-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in wait within]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text accept implicit-wait]]
            [malt-admin.config :as c])
  (:import (org.openqa.selenium.support.ui ExpectedConditions)))

(deftest models-test
  (t/with-system [s (test-system @c/config)]
                 (let [b (t/start-browser! s)]
                   (signin)
                   (go "/files")

                   (let [id (rand-int 1000000)
                         model-selector (keyword (format ".model[data-id='%d']" id))]
                     (testing "Model uploading"
                       (is (empty? (elements model-selector)))

                       (click "Upload New")
                       (fill-in "ID" (str id))
                       (send-keys "File" *file*)
                       (click "Submit")
                       (Thread/sleep 300)
                       (is (seq (elements  model-selector))))

                     (testing "Replace"
                       (within b model-selector
                               (click "Replace"))
                       (send-keys "File" "/etc/hosts")
                       (click "Submit")
                       (t/wait-condition (ExpectedConditions/alertIsPresent))
                       (accept)
                       (wait 100)
                       (within b model-selector
                               (is (= "hosts" (text :.model-file-name)))))

                     (testing "Download"
                       (within b model-selector
                               (click "Download")
                               (t/wait-condition b (ExpectedConditions/alertIsPresent))
                               (accept b)
                               (wait 150)))

                     (testing "Delete"
                       (within b model-selector
                               (click "Delete")
                               (t/wait-condition b (ExpectedConditions/alertIsPresent))
                               (accept b)
                               (wait 100))
                       (is (empty? (elements model-selector))))))))
