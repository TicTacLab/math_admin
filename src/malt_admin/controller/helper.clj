(ns malt-admin.controller.helper
  (:require [ring.util.response :as res]))

(defn redirect-back [req]
  (let [back (get-in req [:headers "referer"])]
    (res/redirect-after-post back)))
