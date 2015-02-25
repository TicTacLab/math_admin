(ns malt-admin.controller.models
  (:require [malt-admin.view :refer (render)]
            [malt-admin.storage.configuration :as st]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [formative.core :as f]))

(defn index [req]
  (render "models/index" req {}))
