(ns malt-admin.config
  (:require [cheshire.core :as json]))

(def cfg (atom nil))

(defn load-config []
  (reset! cfg (json/parse-string (slurp "config.json") true)))

(defn config []
  (if-let [c @cfg]
    c
    (do
      (load-config)
      @cfg)))