(ns malt-admin.test-helper
  (:use clojure.test
        clj-webdriver.taxi)
  (:require [malt-admin.system :as sys]
            [malt-admin.embedded-storage :refer (map->EmbeddedStorage)]
            [malt-admin.helpers :refer [csv-to-list]]
            [clj-webdriver.core :as webdriver]
            [clj-webdriver.element :refer [element-like?]]
            [com.stuartsierra.component :as component]))

(def browser (atom nil))
(def base-url (atom nil))

(defn start-browser! [system]
  (swap! base-url (constantly (format "http://localhost:%d"
                                      (get-in system [:web :port]))))
  (or @browser
      (swap! browser (constantly (set-driver! {:browser :chrome})))))

(defn stop-browser! []
  (swap! browser (constantly nil))
  (swap! base-url (constantly nil))
  (quit))

(.. Runtime
    (getRuntime)
    (addShutdownHook (Thread. stop-browser!)))

(set-finder! (fn finder
               ([q]
                 (finder *driver* q))
               ([driver q]
                (cond
                  (element-like? q) q
                  (keyword? q) (css-finder driver (name q))
                  (string? q) (or (not-empty (xpath-finder driver (format "//a[text()='%s']" q)))
                                  (not-empty (xpath-finder driver (format "//button[text()='%s']|//input[@type='submit' and @value='%s']" q q)))
                                  (let [labels (xpath-finder driver
                                                            (format "//label[text()='%s']" q))
                                        id (webdriver/attribute (first labels) :for)]
                                    (not-empty (xpath-finder driver (format "//*[@id='%s']" id)))))))))

(defn go [browser & [url]]
  (to browser (str @base-url url)))

(defn fill-in [browser q text]
  (clear browser q)
  (input-text browser q text))

(defn signin [browser]
  (doto browser
    (go "/auth")
    (input-text :#field-login "admin")
    (input-text :#field-password "admin1488")

    (submit :#field-submit)))

(defn test-system [{:keys [storage-nodes
                           storage-keyspace
                           configuration-table
                           settings-table] :as conf}]
  (-> (sys/new-system conf)
      (assoc :storage (map->EmbeddedStorage {:storage-nodes       (csv-to-list storage-nodes)
                                             :storage-keyspace    storage-keyspace
                                             :configuration-table configuration-table
                                             :settings-table      settings-table
                                             }))))

(defmacro with-system [[nm system] & body]
  `(let [~nm (component/start ~system)]
     (try 
       ~@body
       (finally
         (component/stop ~nm)))))