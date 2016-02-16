(ns malt-admin.model-validator
  (:require [malcolmx.core :as mx]
            [clojure.zip :as zip]
            [instaparse.core :as insta])
  (:import (org.apache.poi.ss.formula WorkbookEvaluator FormulaType)
           (org.apache.poi.ss.usermodel Cell)
           (org.apache.poi.ss.util CellReference)))


(def supported (set (WorkbookEvaluator/getSupportedFunctionNames)))

(def parser (-> (slurp "src/malt_admin/model_validator/formula-grammar.bnf")
                (insta/parser :auto-whitespace :standard)))

(def branch? vector?)

(def children (fn [[_name & children]] children))

(def make-node (fn [node children]
                 (vec (concat node children))))

(defn parse [formula]
  (let [res (insta/parse parser formula :partial true)]
    (when (string? res)
      (println res))
    res))

(defn reduce-by-fun-name [func init tree]
  (let [hiccup-zipper (zip/zipper branch? children make-node tree)]
    (loop [zp hiccup-zipper
           acc init]
      (if (zip/end? zp)
        acc
        (if (and (zip/branch? zp)
                 (= :FUNCTION (first (zip/node zp))))
          (recur (zip/next zp) (func acc (second (zip/node zp))))
          (recur (zip/next zp) acc))))))

(defn collect-not-supported [tree]
  (reduce-by-fun-name (fn [acc fun-nm]
                        (if (contains? supported fun-nm)
                          acc
                          (conj acc fun-nm)))
                      []
                      tree))

(defn as-a1 [cell]
  (let [cell-ref (CellReference. (.getSheetName (.getSheet cell))
                                 (.getRowIndex cell)
                                 (.getColumnIndex cell)
                                 false false)]
    (.formatAsString cell-ref)))

;; ====================
;; ===== Public =======

(defn has-sheet? [file sheet]
  (try
    ((set (mx/get-sheets-names file)) sheet)
    (catch Exception _ false)))

(defn ids-are-numbers? [file sheet]
  (try
    (->> (mx/get-sheet-with-row file sheet)
         (map #(get % "id"))
         (every? number?))
    (catch Exception _ false)))

(defn ids-are-unique? [file sheet]
  (try
    (let [ids (->> (mx/get-sheet-with-row file sheet)
                   (map #(get % "id")))
          unique-ids (set ids)]
      (= (count ids)
         (count unique-ids)))
    (catch Exception _ false)))

(defn has-column? [file sheet column]
  (try
    (->> (mx/get-sheet-with-row file sheet)
         (every? #(get % column)))
    (catch Exception _ false)))

(defn has-not-supported-functions? [file]
  (->> (for [sheet (seq file)
             row (seq sheet)
             cell (seq row)
             :when (= Cell/CELL_TYPE_FORMULA (.getCellType cell))
             :let [formula (.getCellFormula cell)
                   cell-coord (as-a1 cell)
                   formula-tree (parse formula)]]
         (when-let [not-sup (seq (collect-not-supported formula-tree))]
           {:cell cell-coord
            :fns  not-sup}))
       (filter seq)))