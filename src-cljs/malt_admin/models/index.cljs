(ns malt-admin.models.index
  (:require [dommy.core :as dom]))

(enable-console-print!)

(defn init-confirmable! []
  (dom/listen! [js/document :.confirmable]
               :click #(when-not (js/confirm "Are you sure?")
                        (.preventDefault %))))

(init-confirmable!)