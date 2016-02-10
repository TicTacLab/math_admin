(ns malt-admin.model-validator
  (:require [malcolmx.core :as mx]))


(defn has-sheet? [model sheet]
  (try
    ((set (mx/get-sheets-names model)) sheet)
    (catch Exception _ false)))

(defn ids-are-numbers? [model sheet]
  (try
    (->> (mx/get-sheet-with-row model sheet)
         (map #(get % "id"))
         (every? number?))
    (catch Exception _ false)))

(defn ids-are-unique? [model sheet]
  (try
    (let [ids (->> (mx/get-sheet-with-row model sheet)
                   (map #(get % "id")))
          unique-ids (set ids)]
      (= (count ids)
         (count unique-ids)))
    (catch Exception _ false)))

(defn has-column? [model sheet column]
  (try
    (->> (mx/get-sheet-with-row model sheet)
         (every? #(get % column)))
    (catch Exception _ false)))