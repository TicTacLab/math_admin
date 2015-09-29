(ns malt-admin.models.index
  (:require [malt-admin.utils :as utils]))

(enable-console-print!)

(defn init-confirmable! []
  (utils/confirmable :.confirmable "Are you sure?"))

(init-confirmable!)