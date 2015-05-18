(ns malt-admin.models-test
  (:use clojure.test)
  (:require [malt-admin.test-helper :as t :refer [test-system signin signout go fill-in]]
            [clj-webdriver.taxi :as w :refer [elements click send-keys text accept implicit-wait]]
            [environ.core :as environ]))

(defmacro within [b q & body]
  `(let [p# (w/element ~b ~q)]
     (prn p#)
     (prn ~q)
     (binding [w/*driver* p#]
       ~@body)))

(deftest models-test
  (t/with-system [s (test-system environ/env)]
    (let [b (t/start-browser! s)]
      (signin)
      (go "/models")

      (let [id (+ 100000 (rand-int 1000000))
            model-selector (keyword (format ".model[data-id='%d']" id))]
        (testing "Model uploading"
          (is (empty? (elements model-selector)))

          (click  "Upload New")
          (fill-in  "ID" (str id))
          (fill-in  "Name" "SuperName")
          (send-keys  "File" *file*)
          (click  "Submit")
          (is (seq (elements  model-selector))))

        (testing "Replace"
          (implicit-wait  500)
          (within b model-selector
            (click "Replace"))
          (fill-in  "In sheet name" "MEGASHIT")
          (fill-in  "Out sheet name" "MEGASHUT")
          (send-keys  "File" "/etc/hosts")
          (click  "Submit")
          (accept )
          (prn "$#@&*^@#$&*^@$#&*" model-selector)
          (within b model-selector
            (is (= "MEGASHIT" (text :.model-in-sheet-name)))
            (is (= "MEGASHUT" (text :.model-out-sheet-name)))
            (is (= "hosts" (text :.model-file-name)))))

       #_ (testing "Download"
          (click "Download")
          (accept b))

        #_(testing "Delete"
          (click b "Delete")
          (accept b)
          (implicit-wait b 100)
          (is (empty? (elements b :.model))))))))
