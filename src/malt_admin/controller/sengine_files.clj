(ns malt-admin.controller.sengine-files
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [malt-admin.view :refer (render u)]
            [clojure.tools.logging :as log]
            [clojure.string :as s]))

(defn wrap-error [log-error-prefix error]
  (if (instance? Throwable error)
    (do
      (log/error error log-error-prefix)
      [:error (.getLocalizedMessage error)])
    (do
      (log/error (str log-error-prefix error))
      [:error error])))

(defn error-response->string-message [response]
  (->> response
       :errors
       (map :message)
       (s/join " ")))

(defn index
  [{{:keys [sengine-addr]} :web :as req}]
  (let [url (format "http://%s/files" sengine-addr)
        {:keys [status body error]} @(http/get url)
        response (json/parse-string body true)
        error-prefix "Error while getting files: "]
    (cond
      error (render "sengine/index" (->> error
                                         (wrap-error error-prefix)
                                         (vector)
                                         (into {})
                                         (assoc req :flash)) {})
      (not= status 200) (render "sengine/index" (->> response
                                                     (error-response->string-message)
                                                     (wrap-error error-prefix)
                                                     (vector)
                                                     (into {})
                                                     (assoc req :flash)) {})
      :else (render "sengine/index" req {:files (:data response)}))))