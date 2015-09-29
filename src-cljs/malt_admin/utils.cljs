(ns malt-admin.utils
  (:require [dommy.core :as dom]))

(defn confirmable [selector text]
  (dom/listen! [js/document selector]
                 :click #(when-not (js/confirm text)
                          (.preventDefault %))))