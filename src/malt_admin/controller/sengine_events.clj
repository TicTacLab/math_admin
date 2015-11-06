(ns malt-admin.controller.sengine-events
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as s]
            [formative.parse :as fp]
            [malt-admin.view :refer (render u render-with-success render-with-error)]
            [malt-admin.helpers :refer [redirect-with-flash error!]]
            [malt-admin.form.model :as form]
            [malt-admin.audit :as audit]))

(defn wrap-error [log-error-prefix error]
  (if (instance? Throwable error)
    (do
      (log/error error log-error-prefix)
      [nil (.getLocalizedMessage error)])
    (do
      (log/error (str log-error-prefix error))
      [nil error])))

(defn error-response->string-message [response]
  (->> response
       :errors
       (map :message)
       (s/join " ")))

(defn check-response
  [resp error-prefix]
  (let [{:keys [status body error]} resp
        response (json/parse-string body true)]
    (cond
      error (wrap-error error-prefix error)
      (not= status 200) (->> response
                             (error-response->string-message)
                             (wrap-error error-prefix))
      :else [(:data response) nil])))

(defn index
  [{{:keys [sengine-addr]} :web :as req}]
  (let [url (format "http://%s/events" sengine-addr)
        resp @(http/get url)
        error-prefix "Error while getting events: "
        [events error] (check-response resp error-prefix)]
    (if error
      (render-with-error "sengine/index" req error {})
      (render "sengine/events/index" req {:events (seq events)}))))