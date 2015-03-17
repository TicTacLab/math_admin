(ns malt-admin.helpers
  (:require [clojure.string :refer [split trim]]
            [ring.util.response :as res]
            [flatland.protobuf.core :as pb])
  (:import (flatland.protobuf PersistentProtocolBufferMap$Def)))

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

(def Packet (pb/protodef outcome.Outcome$Packet
                         {:naming-strategy PersistentProtocolBufferMap$Def/protobufNames}))

