(ns malt-admin.models.index
  (:require [dommy.core :as dom]))

(enable-console-print!)

(defn init-confirmable! []
  (doseq [el (dom/sel :.confirmable)]
    (dom/listen! el :click (fn [e]
                             (when-not (js/confirm "Are you sure?")
                               (.preventDefault e))))))

(init-confirmable!)