(ns malt-admin.controller.configuration
  (:require [malt-admin.view :refer (render)]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [formative.core :as f]))

(defn index [req]
  (render "configuration/index" req {}))
