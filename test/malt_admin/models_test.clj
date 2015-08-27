(ns malt-admin.models-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in wait within]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text accept implicit-wait]]
            [malt-admin.config :as c]))

(deftest models-test
  (t/with-system [s (test-system @c/config)]
                 (let [b (t/start-browser! s)]
                   (signin)
                   (go "/models")

                   (let [id (rand-int 1000000)
                         model-selector (keyword (format ".model[data-id='%d']" id))]
                     (testing "Model uploading"
                       (is (empty? (elements model-selector)))

                       (click "Upload New")
                       (fill-in "ID" (str id))
                       (fill-in "Name" "SuperName")
                       (send-keys "File" *file*)
                       (click "Submit")
                       (is (seq (elements  model-selector))))

                     (testing "Replace"
                       (within b model-selector
                               (click "Replace"))
                       (fill-in "In sheet name" "MEGASHIT")
                       (fill-in "Out sheet name" "MEGASHUT")
                       (send-keys "File" "/etc/hosts")
                       (click "Submit")
                       (accept)
                       (wait 100)
                       (within b model-selector
                               (is (= "MEGASHIT" (text :.model-in-sheet-name)))
                               (is (= "MEGASHUT" (text :.model-out-sheet-name)))
                               (is (= "hosts" (text :.model-file-name)))))

                     (testing "Download"
                       (within b model-selector
                               (click "Download")
                               (accept b)
                               (wait 100)))

                     (testing "Delete"
                       (within b model-selector
                               (click "Delete")
                               (accept b)
                               (wait 100))
                       (is (empty? (elements model-selector))))))))
