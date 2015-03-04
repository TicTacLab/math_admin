(ns malt-admin.test-helper
  (:use clojure.test
        clj-webdriver.taxi)
  (:require [malt-admin.system :as sys]
            [malt-admin.embedded-storage :refer (map->EmbeddedStorage)]
            [malt-admin.helpers :refer [csv-to-list]]
            [clj-webdriver.element :refer [element-like?]]))

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
                  (string? q) (xpath-finder driver
                                            (format "//*[text()='%s']|//*[@value='%s']" q q))))))

(defn go [browser & [url]]
  (to browser (str @base-url url)))

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