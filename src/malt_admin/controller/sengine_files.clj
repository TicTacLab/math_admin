(ns malt-admin.controller.sengine-files
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]
            [clojure.string :as s]
            [formative.parse :as fp]
            [malt-admin.view :refer (render u)]
            [malt-admin.helpers :refer [redirect-with-flash error!]]
            [malt-admin.form.model :as form]
            [malt-admin.audit :as audit]))

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
                            (not= status 201) (render "sengine/upload" (->> response
                                                                            (error-response->string-message)
                                                                            (wrap-error error-prefix)
                                                                            (vector)
                                                                            (into {})
                                                                            (assoc req :flash)) {})
                            :else (do
                                    (audit/info req :upload-sengine-file (dissoc values :file))
                                    (redirect-with-flash "/sengine/files" {:success "DONE"}))))))))


