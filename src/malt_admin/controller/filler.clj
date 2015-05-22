(ns malt-admin.controller.filler
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.cache-q :as cache-q]
            ))

(defn index [{{storage :storage} :web
              :as req}]
  (let [q-size (cache-q/get-queue-count storage)]
    (render "filler/index" req {:q-size q-size})))
