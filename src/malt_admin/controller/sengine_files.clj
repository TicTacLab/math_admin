(ns malt-admin.controller.sengine-files
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as s]
            [formative.parse :as fp]
            [malt-admin.view :refer (render u)]
            [malt-admin.helpers :refer [redirect-with-flash error!]]
            [malt-admin.form.model :as form]
            [malt-admin.audit :as audit]
            [clojure.java.io :as io])
  (:import (java.util UUID)))

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

(defn upload [{:keys [problems params] :as req}]
  (render "sengine/upload" req {:upload-form (assoc form/upload-form
                                              :action "/sengine/files"
                                              :method "POST"
                                              :values params
                                              :problems problems)}))

(defn- check-empty-file
  [{:keys [file] :as params}]
  (if (zero? (get file "size"))
    (dissoc params :file)
    params))

(defn do-upload
  [{params :params
    {:keys [sengine-addr]} :web
    :as req}]
  (fp/with-fallback #(upload (assoc req :problems %))
    (let [values (check-empty-file (fp/parse-params form/upload-form params))
          error-prefix "Error while uploading file to sengine: "]
      (cond
        (not (contains? values :file))
        (error! [:file] "You should specify file!")

        :else
        (let [{:keys [id file]} values
              {:strs [tempfile filename]} file
              url (format "http://%s/files/%s/upload" sengine-addr id)
              {:keys [status body error]} @(http/post url {:multipart [{:name     "file"
                                                                        :content  tempfile
                                                                        :filename filename}]})
              response (json/parse-string body true)]
          (cond
            error (render "sengine/upload" (->> error
                                                (wrap-error error-prefix)
                                                (vector)
                                                (into {})
                                                (assoc req :flash)) {})
            (not= status 200) (render "sengine/upload" (->> response
                                                            (error-response->string-message)
                                                            (wrap-error error-prefix)
                                                            (vector)
                                                            (into {})
                                                            (assoc req :flash)) {})
            :else (do
                    (audit/info req :upload-sengine-file (dissoc values :file))
                    (redirect-with-flash "/sengine/files" {:success "DONE"}))))))))

(defn edit
  [{:keys [params] :as req}]
  (let [{:keys [id]} params]
    (->> {:edit-form (assoc form/edit-form :values params
                                           :action (format "/sengine/files/%s/edit" id)
                                           :method "POST")}
         (render "sengine/edit" req))))

(defn do-edit
  [{:keys [params web] :as req}]
  (fp/with-fallback #(upload (assoc req :problems %))
    (let [{:keys [sengine-addr]} web
          values (check-empty-file (fp/parse-params form/upload-form params))
          error-prefix "Error while uploading file to sengine: "]
      (cond
        (not (contains? values :file))
        (error! [:file] "You should specify file!")

        :else
        (let [{:keys [id file]} values
              {:strs [tempfile filename]} file
              url (format "http://%s/files/%s" sengine-addr id)
              {:keys [status body error]} @(http/post url {:multipart [{:name     "file"
                                                                        :content  tempfile
                                                                        :filename filename}]})
              response (json/parse-string body true)]
          (cond
            error (render "sengine/upload" (->> error
                                                (wrap-error error-prefix)
                                                (vector)
                                                (into {})
                                                (assoc req :flash)) {})
            (not= status 200) (render "sengine/upload" (->> response
                                                            (error-response->string-message)
                                                            (wrap-error error-prefix)
                                                            (vector)
                                                            (into {})
                                                            (assoc req :flash)) {})
            :else (do
                    (audit/info req :replace-sengine-file (dissoc values :file))
                    (redirect-with-flash "/sengine/files" {:success "DONE"}))))))))

(defn delete
  [{:keys [web params] :as req}]
  (let [{:keys [sengine-addr]} web
        {:keys [id]} params
        url (format "http://%s/files/%s" sengine-addr id)
        {:keys [status body error]} @(http/delete url)
        response (json/parse-string body true)
        error-prefix "Error while deleting file from sengine: "]
    (cond
      error (->> error
                 (wrap-error error-prefix)
                 (vector)
                 (into {})
                 (redirect-with-flash "/sengine/files"))
      (not= status 200) (->> response
                             (error-response->string-message)
                             (wrap-error error-prefix)
                             (vector)
                             (into {})
                             (redirect-with-flash "/sengine/files"))
      :else (do
              (audit/info req :delete-sengine-file {:id id})
              (redirect-with-flash "/sengine/files"
                                   {:success (format "File with id %s was deleted" id)})))))

(defn download
  [{:keys [params web] :as req}]
  (let [{:keys [id]} params
        {:keys [sengine-addr]} web
        url (format "http://%s/files/%s" sengine-addr id)
        {:keys [status body error headers]} @(http/get url)
        error-prefix "Error while downloading file from sengine: "]
    (cond
      error (->> error
                 (wrap-error error-prefix)
                 (vector)
                 (into {})
                 (redirect-with-flash "/sengine/index"))
      (not= status 200) (as-> (String. body) $
                              (json/parse-string $ true)
                              (error-response->string-message $)
                              (wrap-error error-prefix $)
                              (vector $)
                              (into {} $)
                              (redirect-with-flash "/sengine/files" $))
      :else (do
              (audit/info req :download-sengine-model {:id id})
              {:body body
               :headers {"Content-Type" (:content-type headers)
                         "Content-Disposition" (:content-disposition headers)}}))))


(defn init-profile-session [{params :params web :web :as req}]
  (let [{:keys [id]} params
        {:keys [sengine-addr]} web
        event-id (str (UUID/randomUUID))
        url (format "http://%s/files/%s/%s" sengine-addr id event-id)
        {:keys [status body error]} @(http/post url)
        error-prefix "Error while creating session: "]
    (cond
      error (->> error
                 (wrap-error error-prefix)
                 (vector)
                 (into {})
                 (redirect-with-flash "/sengine/files"))
      (not= status 200) (as-> body $
                              (json/parse-string $ true)
                              (error-response->string-message $)
                              (wrap-error error-prefix $)
                              (vector $)
                              (into {} $)
                              (redirect-with-flash "/sengine/files" $))
      :else (do
              (redirect-with-flash (format "/sengine/files/%s/profile/%s" id event-id) {})))))

(defn view-profile [req]
  (render "sengine/profile" req {}))

