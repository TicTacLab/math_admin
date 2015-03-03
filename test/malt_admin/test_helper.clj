(ns malt-admin.test-helper
  (:use clojure.test
        kerodon.core
        kerodon.test)
  (:require [malt-admin.system :as sys]
            [malt-admin.embedded-storage :refer (map->EmbeddedStorage)]
            [malt-admin.helpers :refer [csv-to-list]]
            [net.cgrand.enlive-html :as enlive])
  (:import (java.io File)))

(defn signin [sess]
  (-> sess
      (visit "/auth")
      (fill-in "Login" "admin")
      (fill-in "Password" "admin1488")
      (press "Sign In")))

(defmacro element? [selector]
  `(validate >
             #(count (enlive/select (:enlive %) ~selector))
             0
             (~'element? ~selector)))

(defn view-in-browser [state]
  (let [file (File/createTempFile "kerodon" ".html")]
    (spit file (get-in state [:response :body]))
    (.. Runtime (getRuntime) (exec (str "xdg-open " (.getAbsolutePath file)))))
  state)

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