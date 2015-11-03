(ns malt-admin.controller.sengine-files
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as s]
            [formative.parse :as fp]
            [malt-admin.view :refer (render u render-with-success render-with-error)]
            [malt-admin.helpers :refer [redirect-with-flash error!]]
            [malt-admin.form.model :as form]
            [malt-admin.audit :as audit])
  (:import (java.util UUID)))

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
  (let [url (format "http://%s/files" sengine-addr)
        resp @(http/get url)
        error-prefix "Error while getting files: "
        [files error] (check-response resp error-prefix)]
    (if error
      (render-with-error "sengine/index" req error {})
      (render "sengine/index" req {:files files}))))

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
    (let [values (check-empty-file (fp/parse-params form/upload-form params))]
      (cond
        (not (contains? values :file))
        (error! [:file] "You should specify file!")
        :else
        (let [{:keys [id file]} values
              {:strs [tempfile filename]} file
              error-prefix "Error while uploading file to sengine: "
              url (format "http://%s/files/%s/upload" sengine-addr id)
              resp @(http/post url {:multipart [{:name     "file"
                                                 :content  tempfile
                                                 :filename filename}]})
              [_ error] (check-response resp error-prefix)]
          (if error
            (render-with-error "sengine/upload" req error {})
            (do
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
          values (check-empty-file (fp/parse-params form/upload-form params))]
      (cond
        (not (contains? values :file))
        (error! [:file] "You should specify file!")
        :else
        (let [{:keys [id file]} values
              {:strs [tempfile filename]} file
              url (format "http://%s/files/%s" sengine-addr id)
              error-prefix "Error while uploading file to sengine: "
              resp @(http/post url {:multipart [{:name     "file"
                                                 :content  tempfile
                                                 :filename filename}]})
              [_ error] (check-response resp error-prefix)]
          (if error
            (render-with-error "sengine/upload" req error {})
            (do
              (audit/info req :replace-sengine-file (dissoc values :file))
              (redirect-with-flash "/sengine/files" {:success "DONE"}))))))))

(defn delete
  [{:keys [web params] :as req}]
  (let [{:keys [sengine-addr]} web
        {:keys [id]} params
        url (format "http://%s/files/%s" sengine-addr id)
        error-prefix "Error while deleting file from sengine: "
        [_ error] (check-response @(http/delete url) error-prefix)]
    (if error
      (redirect-with-flash "/sengine/files" {:error error})
      (do
        (audit/info req :delete-sengine-file {:id id})
        (redirect-with-flash "/sengine/files" {:success (format "File with id %s was deleted" id)})))))

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


(defn init-profile-session
  [{:keys [web params]}]
  (let [{:keys [id]} params
        {:keys [sengine-addr]} web
        event-id (str (UUID/randomUUID))
        url (format "http://%s/files/%s/%s" sengine-addr id event-id)
        error-prefix "Error while creating session: "
        [_ error] (check-response @(http/post url) error-prefix)]
    (if error
      (redirect-with-flash "/sengine/files" {:error error})
      (redirect-with-flash (format "/sengine/files/%s/profile/%s" id event-id) {}))))

(defn render-profile [req event-log out admin?]
  (let [event-log-rows (->> event-log
                            (map (fn [row]
                                   (->> row
                                        (remove (comp empty? val))
                                        (into {}))))
                            (mapv json/generate-string))
        out-header-keys (->> (first out)
                             (keys)
                             (vec))
        out-rows (map (fn [row] (map #(get row %) out-header-keys)) out)]
    (render "sengine/profile" req {:admin? admin?
                                   :event-log event-log-rows
                                   :out {:header (mapv name out-header-keys)
                                         :rows out-rows}})))

(defn view-profile
  [{:keys [params web] :as r}]
  (let [{:keys [id event-id]} params
        {:keys [sengine-addr]} web
        url (format "http://%s/files/%s/%s/event-log"
                    sengine-addr id event-id)
        error-prefix "Error while getting event log: "
        [event-log error] (check-response @(http/get url) error-prefix)]
    (if error
      (redirect-with-flash "/sengine/files" {:error error})
      (let [url (format "http://%s/files/%s/%s/settlements"
                        sengine-addr id event-id)
            resp @(http/get url)
            error-prefix "Error while getting settlements: "
            [out error] (check-response resp error-prefix)
            admin? (get-in r [:session :is-admin] false)]
        (if error
          (redirect-with-flash "/sengine/files" {:error error})
          (render-profile r event-log out admin?))))))

(defn send-profile
  [{:keys [params web] :as r}]
  (let [{:keys [send-type event-log id event-id]} params
        {:keys [sengine-addr]} web
        url (format "http://%s/files/%s/%s/event-log/%s"
                    sengine-addr id event-id send-type)
        resp @(http/post url {:body event-log})
        error-prefix "Error while setting event log: "
        [_ error] (check-response resp error-prefix)]
    (if error
      (redirect-with-flash (:uri r) {:error error})
      (let [url (format "/sengine/files/%s/profile/%s" id event-id)]
        (redirect-with-flash url {:success "DONE"})))))

(defn destroy-profile-session
  [{:keys [params web] :as r}]
  (let [{:keys [id event-id]} params
        {:keys [sengine-addr]} web
        url (format "http://%s/files/%s/%s" sengine-addr id event-id)
        resp @(http/delete url)
        error-prefix "Error while deleting session: "
        [_ error] (check-response resp error-prefix)]
    (if error
      (redirect-with-flash (format "/sengine/files/%s/profile/%s" id event-id) {:error error})
      (redirect-with-flash "/sengine/files" {:success "DONE"}))))

(defn get-profile-workbook
  [{:keys [params web] :as req}]
  (let [{:keys [sengine-addr]} web
        {:keys [id event-id]} params
        url (format "http://%s/files/%s/%s" sengine-addr id event-id)
        {:keys [status body error headers]} @(http/get url)
        error-prefix "Error while downloading file from sengine: "]
    (cond
      error (->> error
                 (wrap-error error-prefix)
                 (vector)
                 (into {})
                 (redirect-with-flash (format "/sengine/files/%s/profile/%s" id event-id)))
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