(ns calls-rate
 (:require [clojure.java.io :as io]
           [clojure.string :as str]))

(defn load-values [file-path]
 (let [data (rest (line-seq (io/reader (io/input-stream file-path))))]
  (->> data
       (map (comp #(Double/valueOf %) last #(str/split % #"\t")))
       (map #(* % 60))
       (reduce +))))

(load-values "src/data-30.text")
(load-values "src/data-29.text")