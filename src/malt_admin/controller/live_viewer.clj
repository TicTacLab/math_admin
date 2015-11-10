(ns malt-admin.controller.live-viewer
  (:require [malt-admin.view :refer (render u)]))

(defn index [{params :params :as req}]
  (render "sengine/events/live_viewer" req {:event-id (:id params)}))
