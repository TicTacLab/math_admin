(ns malt-admin.models.edit
  (:require [malt-admin.utils :as utils]))

(defn init-confirmable! []
  (utils/confirmable :#field-submit "Are you sure?"))

(init-confirmable!)