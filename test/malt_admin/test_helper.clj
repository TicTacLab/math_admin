(ns malt-admin.test-helper
  (:use clojure.test)
  (:require [clj-webdriver.core :as webdriver]
            [clj-webdriver.element :refer [element-like?]]
            [clj-webdriver.taxi :as w]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [malt-admin.system :as sys]
            [clojure.string :as s])
  (:import (java.io File)))

(defonce browser (atom nil))
(defonce base-url (atom nil))

(defn start-browser! [system]
  (swap! base-url (constantly (format "http://localhost:%d"
                                      (get-in system [:web :port]))))
  (or @browser
      (swap! browser (constantly (w/set-driver! {:browser :chrome})))))

(defn stop-browser! []
  (swap! browser (constantly nil))
  (swap! base-url (constantly nil))
  (w/quit))

(comment (stop-browser!) )

(defn not-nil-elements [s]
  (->> s
       (remove #(not (:webelement %)))
       (not-empty)))

(.. Runtime
    (getRuntime)
    (addShutdownHook (Thread. stop-browser!)))

(w/set-finder! (fn finder
                 ([q]
                  (finder w/*driver* q))
                 ([driver q]
                  (cond
                    (element-like? q) q
                    (keyword? q) (w/css-finder driver (name q))
                    (sequential? q) (w/css-finder driver (str/join " " (map name q)))
                    (string? q) (or (not-nil-elements (w/xpath-finder driver (format ".//a[text()='%s']" q)))
                                    (not-nil-elements (w/xpath-finder driver (format ".//input[@type='submit' and @value='%s']" q)))
                                    (not-nil-elements (w/xpath-finder driver (format ".//button[text()='%s']" q)))
                                    (let [labels (w/xpath-finder driver
                                                                 (format ".//label[text()='%s']|.//label[./*[text()='%s']]" q q))
                                          id (webdriver/attribute (first labels) :for)]
                                      (not-nil-elements (w/xpath-finder driver (format ".//*[@id='%s']" id)))))))))

(defn go
  ([url]
   (go w/*driver* url))
  ([browser & [url]]
   (w/to browser (str @base-url url))))

(defmacro within [b q & body]
  `(let [p# (w/element ~b ~q)]
     (binding [w/*driver* p#]
       ~@body)))

(defn wait-condition
  ([b expected-condition]
   (w/wait-until b #(.apply expected-condition (:webdriver %))
                 1000 1000))
  ([expected-condition]
   (w/wait-until w/*driver* #(.apply expected-condition (:webdriver %))
                 1000 1000)))

(defn fill-in
  ([q text]
   (fill-in w/*driver* q text))
  ([browser q text]
   (w/clear browser q)
   (w/input-text browser q text)))

(defn signin
  ([]
   (signin w/*driver*))
  ([browser]
   (signin browser "admin" "admin1488"))
  ([browser login password]
   (doto browser
     (go "/auth")
     (w/input-text :#field-login login)
     (w/input-text :#field-password password)

     (w/submit :#field-submit))))

(defn signout
  ([]
   (signout w/*driver*))
  ([browser]
   (w/click browser "Sign out")))

(defn wait [ms]
  (Thread/sleep ms))

(defn test-system [conf]
  (sys/new-system conf))

(defmacro with-system [[nm system] & body]
  `(let [~nm (component/start ~system)]
     (try
       ~@body
       (finally
         (component/stop ~nm)))))

(defn get-file-path [model-name]
  (s/join (File/separator) [(System/getProperty "user.dir") "test" "resources" model-name]))
