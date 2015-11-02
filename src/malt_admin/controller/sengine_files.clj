(ns malt-admin.controller.sengine-files
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [malt-admin.view :refer (render u)]))

(defn index
  [{{:keys [sengine-addr]} :web :as req}]
  (let [url (format "http://%s/files" sengine-addr)
        {:keys [status body]} @(http/get url)
        files (:data (json/parse-string body true))]
    (>trace files)
    (render "sengine/index" req {:files files})))