(ns malt-admin.mengine-files-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in wait within]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text accept implicit-wait]]
            [malt-admin.config :as c])
  (:import (org.openqa.selenium.support.ui ExpectedConditions)
           (java.util UUID)))

(deftest models-test
  (t/with-system [s (test-system (c/config))]
                 (let [b (t/start-browser! s)]
                   (signin)
                   (go "/mengine/files")

                   (let [id (rand-int 1000000)
                         model-name (str (UUID/randomUUID))
                         model-selector (keyword (format ".model[data-id='%d']" id))]
                     (testing "Model uploading"
                       (is (empty? (elements model-selector)))

                       (click "Upload New")
                       (fill-in "ID" (str id))
                       (send-keys "File" *file*)
                       (fill-in "Name" model-name)
                       (click "Submit")
                       (Thread/sleep 300)
                       (is (seq (elements  model-selector)))
                       (within b model-selector
                         (is (= model-name (text :.model-name)))))

                     (testing "Replace"
                       (within b model-selector
                         (click "Replace"))
                       (send-keys "File" "/etc/hosts")
                       (fill-in "Name" (str model-name \!))
                       (click "Submit")
                       (t/wait-condition (ExpectedConditions/alertIsPresent))
                       (accept)
                       (wait 100)
                       (within b model-selector
                         (is (= "hosts" (text :.model-file-name)))
                         (is (= (str model-name \!)
                                (text :.model-name)))))

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
