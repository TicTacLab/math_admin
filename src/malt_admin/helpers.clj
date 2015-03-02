(ns malt-admin.helpers
  (:require [clojure.string :refer [split trim]]
            [ring.util.response :as res]))

(defn csv-to-list [csv]
  (->> (split csv #",")
       (map trim)
       (remove empty?)))

(defn redirect-with-flash [url flash]
  (assoc (res/redirect-after-post url) :flash flash))

(defn error! [& kvs]
  (let [problems (->> kvs
                      (partition 2)
                      (map #(zipmap [:keys :msg] %)))]
    (throw (ex-info "Error parsing params!" {:problems problems}))))
