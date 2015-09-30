(ns malt-admin.controller.models-test
  (:require [clojure.test :refer :all]
            [malt-admin.controller.models :refer :all]
            [malt-admin.test-helper :refer :all]
            [malt-admin.system :as sys]
            [malt-admin.config :as c])
  (:import (java.io File)
           (java.util UUID)))

(deftest models-test
  (testing "upload wizard"
    (testing "load-file step"
      (testing "file size"
        (with-system [s (sys/new-system @c/config)]
          (let [web (:web s)]
            (is (= {:errors ["File is too big"]}
                   (upload-draft* web {:file         (File. *file*)
                                       :file-name    "Soccer.xlsx"
                                       :content-type "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                       :size         (+ 10 (* (:max-file-size web) 1024 1024))}
                                  (str (UUID/randomUUID))))
                "File should not be bigger than max")

            (is (= {:errors ["File should be .xls or .xlsx type"]}
                   (upload-draft* web {:file         (File. *file*)
                                       :file-name    "Soccer.xlsx"
                                       :content-type "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                       :size         10}
                                  (str (UUID/randomUUID))))
                "File should not be xls or xlsx")))))))