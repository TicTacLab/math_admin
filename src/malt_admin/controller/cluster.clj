(ns malt-admin.controller.cluster
  (:require [malt-admin.view :refer (render)]
            [formative.parse :as fp]
            [ring.util.response :as res]
            [formative.core :as f]))

(defn index [req]
  (render "cluster/index" req {}))
