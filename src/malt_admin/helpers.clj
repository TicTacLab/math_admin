(ns malt-admin.helpers
  (:require [clojure.string :refer [split trim]]))

(defn csv-to-list [csv]
  (->> (split csv #",")
       (map trim)
       (remove empty?)))
