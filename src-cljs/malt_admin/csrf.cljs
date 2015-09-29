(ns malt-admin.csrf
  (:require [dommy.core :as dom]
            [clojure.string :as str]))

(defn append-csrf-inputs! []
  (let [token (second (re-find #"csrf=([^;]*);?" js/document.cookie))]
    (doseq [form (dom/sel :form)]
      (let [method (dom/attr form "method")]
        (when (and method (= "post" (str/lower-case method)))
          (dom/append! form
                       (-> (dom/create-element "input")
                           (dom/set-attr! :type "hidden")
                           (dom/set-attr! :name "csrf")
                           (dom/set-attr! :value token))))))))

(append-csrf-inputs!)

