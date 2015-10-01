(ns malt-admin.models.upload.select-in-cells
  (:require [cljs-http.client :as http])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def step
  {:name      "Select In Cells"
   :state-fn  (fn [] {:errors #{}
                      :valid? false})
   :body-comp (fn [state]
                (fn []
                  ))

   :next-fn   (constantly true)
   :valid-fn  :valid?})