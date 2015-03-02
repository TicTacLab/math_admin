(ns malt-admin.helpers
  (:require [clojure.string :refer [split trim]]
            [ring.util.response :as res]))

(defn csv-to-list [csv]
  (->> (split csv #",")
       (map trim)
       (remove empty?)))

(defn redirect-with-flash [url flash]
  (assoc (res/redirect-after-post url) :flash flash))
