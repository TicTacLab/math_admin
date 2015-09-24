(ns malt-admin.password
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (org.owasp.passfault TimeToCrack PasswordAnalysis SequentialFinder BuildFinders DateFinder RandomClassesFinder)
           (org.owasp.passfault.keyboard KeySequenceFinder EnglishKeyBoard RussianKeyBoard)
           (java.io ByteArrayInputStream SequenceInputStream)))

(def machine (doto TimeToCrack/GPU1000
               (.setHashType "ms" 1)))

(def examples ["$R. eallyStrongPassordupto12332"])
(def examples-is (as-> examples $
                       (str/join \newline $)
                       (str \newline $ \newline)
                       (.getBytes $ "UTF-8")
                       (ByteArrayInputStream. $)))

(def finder
  (let [english-is (io/input-stream (io/resource "org/owasp/passfault/dictionary/english.words"))
        dictionary-is (SequenceInputStream. examples-is english-is)
        finders (doto (BuildFinders/buildDictionaryFinders "English" dictionary-is)
                  (.add (KeySequenceFinder. (EnglishKeyBoard.)))
                  (.add (KeySequenceFinder. (RussianKeyBoard.)))
                  (.add (DateFinder.))
                  (.add (RandomClassesFinder.)))]
    (SequentialFinder. finders)))

(defn nano2grade
  "Given nanoseconds to crack it returns
   the grade of password using next gradation:
     up to 1 year              - 0
     from 1 year up to 3 years - 1
     from 3 years              - 3"
  [nano]
  (let [years (/ nano 1e9 60 60 24 30.5 12)]
    (cond
      (<= years 1)   1
      (<= 1 years 3) 2
      :else          3)))

(defn analyze [pass]
  (if-not (seq pass)
    1
    (let [analysis (PasswordAnalysis. pass)]
     (.blockingAnalyze finder analysis)
     (->> analysis
          (.calculateHighestProbablePatterns)
          (.getTotalCost)
          (.getTimeToCrackNanoSeconds machine)
          (nano2grade)))))