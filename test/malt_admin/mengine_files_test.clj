(ns malt-admin.mengine-files-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in wait within]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text accept implicit-wait]]
            [malt-admin.config :as c]
            [clojure.string :as s])
  (:import (java.util UUID)
           (java.io File)
           (org.openqa.selenium.support.ui ExpectedConditions)))

(defn get-model-path [model-name]
  (s/join (File/separator) [(System/getProperty "user.dir") "test" "resources" model-name]))

(def no-in-model-path (get-model-path "test-model-no-in.xls"))
(def no-out-model-path (get-model-path "test-model-no-out.xls"))
(def non-unique-ids-model (get-model-path "test-model-in-non-unique-id.xls"))
(def no-id-column-model (get-model-path "test-model-in-no-id.xls"))
(def no-value-column-model (get-model-path "test-model-no-in-value.xls"))
(def non-number-ids-model (get-model-path "test-model-in-id-non-int.xls"))
(def valid-model-path (get-model-path "test-model.xls"))


(deftest models-test
  (t/with-system [s (test-system (c/config))]
    (let [b (t/start-browser! s)]
      (signin)
      (go "/mengine/files")

      (let [id (rand-int 1000000)
            model-description (str (UUID/randomUUID))
            model-selector (keyword (format ".model[data-id='%d']" id))]
        (testing "Model uploading"
          (is (empty? (elements model-selector)))
          (click "Upload New")
          (fill-in "ID" (str id))
          (send-keys "File" valid-model-path)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)
          (is (seq (elements model-selector)))
          (within b model-selector
            (is (= model-description (text :.model-description)))))


        (testing "Replace with not a model"
          (within b model-selector
            (click "Replace"))
          (send-keys "File" "/etc/hosts")
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: File should be a model"
                 (text b :.control-label))))

        (testing "Replace with model with no IN sheet"
          (send-keys "File" no-in-model-path)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: Model should have IN sheet"
                 (text b :.control-label))))

        (testing "Replace with model with no OUT sheet"
          (send-keys "File" no-out-model-path)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: Model should have OUT sheet"
                 (text b :.control-label))))

        (testing "Replace with model with non unique ids"
          (send-keys "File" non-unique-ids-model)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: Ids on IN sheet must be unique"
                 (text b :.control-label))))

        (testing "Replace with model with non number ids"
          (send-keys "File" non-number-ids-model)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: Ids on IN sheet must be numbers"
                 (text b :.control-label))))

        (testing "Replace with model without id column"
          (send-keys "File" no-id-column-model)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: IN sheet must contain id column"
                 (text b :.control-label))))

        (testing "Replace with model without value column"
          (send-keys "File" no-value-column-model)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: IN sheet must contain value column"
                 (text b :.control-label))))

        (testing "Download"
          (go "/mengine/files")
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
          (is (empty? (elements model-selector))))

        (testing "Model uploading with no IN sheet"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" no-in-model-path)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: Model should have IN sheet"
                 (text b :.control-label))))

        (testing "Model uploading with no OUT sheet"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" no-out-model-path)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: Model should have OUT sheet"
                 (text b :.control-label))))

        (testing "Model uploading not a model"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" *file*)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: File should be a model"
                 (text b :.control-label))))

        (testing "Model uploading with non unique ids"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" non-unique-ids-model)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: Ids on IN sheet must be unique"
                 (text b :.control-label))))

        (testing "Model uploading with non number ids"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" non-number-ids-model)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: Ids on IN sheet must be numbers"
                 (text b :.control-label))))

        (testing "Model uploading without id column"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" no-id-column-model)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: IN sheet must contain id column"
                 (text b :.control-label))))

        (testing "Model uploading without value column"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" no-value-column-model)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: IN sheet must contain value column"
                 (text b :.control-label))))

        ))))
