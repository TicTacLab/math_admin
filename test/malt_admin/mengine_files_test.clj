(ns malt-admin.mengine-files-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [get-file-path test-system signin signout go fill-in wait within]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text accept implicit-wait]]
            [malt-admin.config :as c])
  (:import (java.util UUID)
           (org.openqa.selenium.support.ui ExpectedConditions)))


(def no-in-model-path (get-file-path "test-model-no-in.xls"))
(def no-out-model-path (get-file-path "test-model-no-out.xls"))
(def non-unique-ids-file (get-file-path "test-model-in-non-unique-id.xls"))
(def no-id-column-file (get-file-path "test-model-in-no-id.xls"))
(def no-value-column-file (get-file-path "test-model-no-in-value.xls"))
(def non-number-ids-file (get-file-path "test-model-in-id-non-int.xls"))
(def not-supported-funs-file-path (get-file-path "not-supported-functions.xls"))
(def valid-file-path (get-file-path "test-model.xls"))


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
          (send-keys "File" valid-file-path)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)
          (is (seq (elements model-selector)))
          (within b model-selector
            (is (= model-description (text :.model-description)))))


        (testing "Replace with not valid file"
          (within b model-selector
            (click "Replace"))
          (send-keys "File" "/etc/hosts")
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: Your file must be of XLS or XLSX type"
                 (text b :.control-label))))

        (testing "Replace with file with no IN sheet"
          (send-keys "File" no-in-model-path)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: Your excel file must contain \"IN\" worksheet. See docs"
                 (text b :.control-label))))

        (testing "Replace with file with no OUT sheet"
          (send-keys "File" no-out-model-path)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: Your excel file must contain \"OUT\" worksheet. See docs"
                 (text b :.control-label))))

        (testing "Replace with file with non unique ids"
          (send-keys "File" non-unique-ids-file)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: \"id\" column of \"IN\" worksheet must contain only unique values. See docs"
                 (text b :.control-label))))

        (testing "Replace with file with non number ids"
          (send-keys "File" non-number-ids-file)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: \"id\" column of \"IN\" worksheet must contain only numbers. See docs"
                 (text b :.control-label))))

        (testing "Replace with file without id column"
          (send-keys "File" no-id-column-file)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: \"IN\" worksheet in your file must contain \"id\" column. See docs"
                 (text b :.control-label))))

        (testing "Replace with file without value column"
          (send-keys "File" no-value-column-file)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: \"IN\" worksheet in your file must contain \"value\" column. See docs"
                 (text b :.control-label))))

        (testing "Replace with file with not supported functions"
          (send-keys "File" not-supported-funs-file-path)
          (fill-in "Description" (str model-description \!))
          (click "Submit")
          (t/wait-condition (ExpectedConditions/alertIsPresent))
          (accept)
          (Thread/sleep 300)
          (is (= "File: This cells contain not supported functions: OUT!D2: [\"BIN2HEX\"]; OUT!D3: [\"BIN2OCT\"]"
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

        (testing "file uploading with no IN sheet"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" no-in-model-path)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: Your excel file must contain \"IN\" worksheet. See docs"
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

          (is (= "File: Your excel file must contain \"OUT\" worksheet. See docs"
                 (text b :.control-label))))

        (testing "Uploading not valid file"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" *file*)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: Your file must be of XLS or XLSX type"
                 (text b :.control-label))))

        (testing "Model uploading with non unique ids"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" non-unique-ids-file)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: \"id\" column of \"IN\" worksheet must contain only unique values. See docs"
                 (text b :.control-label))))

        (testing "Model uploading with non number ids"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" non-number-ids-file)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: \"id\" column of \"IN\" worksheet must contain only numbers. See docs"
                 (text b :.control-label))))

        (testing "Model uploading without id column"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" no-id-column-file)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: \"IN\" worksheet in your file must contain \"id\" column. See docs"
                 (text b :.control-label))))

        (testing "Model uploading without value column"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" no-value-column-file)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: \"IN\" worksheet in your file must contain \"value\" column. See docs"
                 (text b :.control-label))))

        (testing "File uploading with unsupported functions"
          (go "/mengine/files")

          (is (empty? (elements model-selector)))

          (click "Upload New")
          (fill-in "ID" (str (rand-int 1000000)))
          (send-keys "File" not-supported-funs-file-path)
          (fill-in "Description" model-description)
          (click "Submit")
          (Thread/sleep 300)

          (is (= "File: This cells contain not supported functions: OUT!D2: [\"BIN2HEX\"]; OUT!D3: [\"BIN2OCT\"]"
                 (text b :.control-label))))))))
