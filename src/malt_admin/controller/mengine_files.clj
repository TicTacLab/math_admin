(ns malt-admin.controller.mengine-files
  (:require [malt-admin.view :refer (render u)]
            [malt-admin.form.model :as form]
            [malt-admin.audit :as audit]
            [malt-admin.storage
             [log :as slog]
             [models :as models]
             [cache :as cache]
             [in-params :as in-params]]
            [cheshire.core :as json]
            [malt-admin.helpers :refer [redirect-with-flash error!]]
            [formative.parse :as fp]
            [org.httpkit.client :as http]
            [clojure.tools.trace :refer [trace]]
            [clojure.string :as s]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.tools.logging :as log]
            [malt-admin.offloader :as off])
  (:refer-clojure :exclude [replace])
  (:import (java.nio.file Files Paths)
           [java.util UUID Date]
           [java.io File]))

(defn index [{{storage :storage} :web :as req}]
  (render "models/index" req {:models (models/get-models storage)}))

(defn upload [{:keys [problems params] :as req}]
  (render "models/upload" req {:upload-form (assoc form/upload-form
                                              :action "/mengine/files"
                                              :method "POST"
                                              :values (merge {:in_sheet_name  "IN"
                                                              :out_sheet_name "OUT"}
                                                             params)
                                              :problems problems)}))

(defn prepare-file-attrs [{{tempfile "tempfile" filename "filename" size "size" content-type "content-type"} :file :as params}]
  (if (zero? size)
    (dissoc params :file)
    (assoc params
      :file (Files/readAllBytes (Paths/get (.toURI ^File tempfile)))
      :file_name filename
      :content_type content-type)))

(defn generate-revision [params]
  (if (:file params)
    (assoc params :rev (str (UUID/randomUUID)))
    params))

(defn add-last-modified [model]
  (if (:file model)
    (assoc model :last_modified (Date.))
    model))

(defn add-default-fields [model]
  (assoc model
    :in_sheet_name "IN"
    :out_sheet_name "OUT"))

(defn do-upload [{params :params
                  {:keys [storage offloader]} :web
                  :as req}]
  (fp/with-fallback #(malt-admin.controller.mengine-files/upload (assoc req :problems %))
    (let [values (->> params
                      (fp/parse-params form/upload-form)
                      (prepare-file-attrs)
                      (add-last-modified)
                      (add-default-fields)
                      (generate-revision))]

      (cond
        (models/model-exists? storage (:id values))
        (error! [:id] (str "Model with this ID already exists: " (:id values)))

        (not (contains? values :file))
        (error! [:file] "You should specify file!")

        :else
        (do
          (models/write-model! storage values)
          (audit/info req :upload-model (dissoc values :file))
          (off/offload-model! offloader (:id values))
          (redirect-with-flash "/mengine/files" {:success "DONE"}))))))

(defn edit [{{storage :storage} :web
             {id :id :as params} :params
             problems :problems
             :as req}]
  (let [model (models/get-model storage (Integer/valueOf ^String id))]
    (render "models/edit" req {:edit-form (assoc form/edit-form
                                            :values (if problems params model)
                                            :action (str "/mengine/files/" (u id))
                                            :method "PUT"
                                            :problems problems)})))

(defn replace [{{:keys [storage offloader]} :web
                params             :params
                :as                req}]
  (fp/with-fallback #(malt-admin.controller.mengine-files/edit (assoc req :problems %))
    (let [parsed-params (fp/parse-params form/edit-form params)
          values (-> parsed-params
                     (prepare-file-attrs)
                     (add-last-modified)
                     (generate-revision)
                     (select-keys [:id :file :name :file_name :content_type :in_sheet_name :out_sheet_name :rev :last_modified]))
          id (:id values)]
      (models/replace-model! storage values)
      (audit/info req :replace-model (dissoc values :file))
      (off/offload-model! offloader id)
      (redirect-with-flash "/mengine/files" {:success (format "File with id %d was replaced" id)}))))

(defn delete [{{id :id}           :params
               {storage :storage} :web
               :as                req}]
  (let [model-id (Integer/valueOf ^String id)
        rev (models/get-rev storage model-id)]
    (models/delete-model! storage model-id)
    (cache/clear storage model-id rev)
    (in-params/delete! storage model-id)
    (audit/info req :delete-model {:id model-id})
    (redirect-with-flash "/mengine/files"
                         {:success (format "File with id %d was deleted"
                                           model-id)})))

(defn download [{{id :id} :params
                 {storage :storage} :web
                 :as req}]
  (let [file (models/get-model-file storage (Integer/valueOf ^String id))]
    (audit/info req :download-model {:id id})
    {:body    (:file file)
     :headers {"Content-Type"        (:content_type file)
               "Content-Disposition" (str "attachment; filename=" (:file_name file))}}))

(defn malt-params->form-fileds [malt-params]
  (let [fields (some->> malt-params
                        (sort-by :id)
                        (map (juxt (comp keyword str :id)
                                   #(format "%s. %s" (:id %) (:code %))
                                   (constantly :text)))
                        (map (partial zipmap [:name :label :type])))
        submit (vector {:name :submit :type :submit :value "Calculate"})]
    (concat submit fields submit)))

(defn malt-params->form-values [malt-params]
  (some->> malt-params
           (map (juxt (comp str :id) :value))
           (into {})))

(defn malt-params->form [malt-params]
  (hash-map :fields (malt-params->form-fileds malt-params)
            :validations (->> malt-params
                              (map (comp keyword str :id))
                              (vector :required)
                              vector)))

(defn make-model-sid [id rev ssid]
  (str ssid \- id \- rev))

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

(defn get-malt-params [api-addr model-id rev ssid]
  (let [url (format "http://%s/files/%s/%s/in-params"
                    api-addr
                    model-id
                    (make-model-sid model-id rev ssid))
        {:keys [status error body]} @(http/get url {:as :text})
        response (json/parse-string body true)
        error-prefix "Error while getting params: "]
    (cond
      error (wrap-error error-prefix error)
      (not= status 200) (wrap-error error-prefix (error-response->string-message response))
      :else [:ok (:data response)])))

(defn get-model-out-values-header [api-addr model-id rev ssid]
  (let [error-prefix "Error while getting header: "
        url (format "http://%s/files/%s/%s/out-values-header"
                    api-addr
                    model-id
                    (make-model-sid model-id rev ssid))
        {:keys [status error body]} @(http/get url {:as :text})
        response (json/parse-string body true)]
    (cond
      error (wrap-error error-prefix error)
      (not= 200 status) (wrap-error error-prefix (error-response->string-message response))
      :else [:ok (->> response :data (map keyword))])))

(defn calculate [api-addr ssid id rev params]
  (let [error-prefix "Error while calculation: "
        malt-session-id (make-model-sid id rev ssid)
        url (format "http://%s/files/%s/%s/profile"
                    api-addr id malt-session-id)
        malt-params {:model_id id
                     :event_id malt-session-id
                     :params   (map (fn [[id value]] {:id id :value value}) params)}
        {:keys [body error status]} @(http/post url {:body    (json/generate-string malt-params)
                                                     :timeout 60000
                                                     :as      :text})
        json-response (json/parse-string body true)]
    (cond
      error (wrap-error error-prefix error)
      (not= status 200) (wrap-error error-prefix (error-response->string-message json-response))
      :else [:ok (:data json-response)])))

(defn remove-invalid-outcomes [outcomes]
  (let [has-valid-coef? (comp number? :coef)]
    (filter has-valid-coef? outcomes)))

(defn format-timer-value [out-value]
  (update-in out-value [:timer] #(format "%.2f" %)))

(defn render-profile-page [req model-id rev & {:keys [problems flash in-params out-params]}]
  (let [api-addr (-> req :web :api-addr)
        model-file (as-> req $
                         (:web $)
                         (:storage $)
                         (models/get-model $ model-id)
                         (:file_name $)
                         (re-matches #"^(.*)\..*$" $)
                         (second $))
        render-with-flash (fn [flash & {:as render-data}]
                            (render "models/profile"
                                    (assoc req :flash flash)
                                    (merge {:model-file  model-file
                                            :total-timer 0} render-data)))
        [malt-params-code malt-params] (get-malt-params api-addr model-id rev (:session-id req))]
    (condp = malt-params-code
      :ok (let [values (or in-params
                           (malt-params->form-values malt-params))
                profile-form (merge (malt-params->form malt-params)
                                    {:action       (str "/mengine/files/" (u model-id) "/" (u rev) "/profile")
                                     :method       "POST"
                                     :values       values
                                     :submit-label "Calculate"
                                     :problems     problems})]
            (if out-params
              (let [[out-header-code out-header] (get-model-out-values-header api-addr model-id rev (:session-id req))]
                (condp = out-header-code
                  :ok (render-with-flash flash
                                         :profile-form profile-form
                                         :total-timer (->> out-params
                                                           :data
                                                           (map :timer)
                                                           (reduce + 0))
                                         :out-values (some->> out-params
                                                              :data
                                                              (map format-timer-value)
                                                              (map (apply juxt (concat out-header [:timer]))))
                                         :out-header (map name out-header))
                  :error (render-with-flash {:error out-header}
                                            :profile-form profile-form)))
              (render-with-flash flash
                                 :profile-form profile-form)))
      :error (render-with-flash {:error malt-params}))))

(defn profile [{params :params :as req}]
  (render-profile-page req (Integer/valueOf ^String (:id params)) (:rev params)))

(defn profile-execute [{session-id :session-id
                        params :params :as req}]
  (let [id (Integer/valueOf ^String (:id params))
        rev (:rev params)
        api-addr (-> req :web :api-addr)
        in-params (dissoc params :id :submit :rev :csrf)
        render (partial render-profile-page req id rev :in-params in-params)
        [malt-params-code malt-params] (get-malt-params api-addr id rev session-id)]
    (condp = malt-params-code
      :ok (fp/with-fallback #(render-profile-page req id
                                                  :problems %
                                                  :in-params in-params)
                            (fp/parse-params (malt-params->form malt-params) in-params)
                            (let [[type result] (calculate api-addr
                                                           session-id
                                                           id
                                                           rev
                                                           in-params)]
                              (condp = type
                                :ok (render :out-params {:data (remove-invalid-outcomes result)})
                                :error (render :flash {:error result}))))
      :error (render-profile-page req id rev :flash {:error malt-params}))))

(defn delete-session [{ssid :session-id
                       {:keys [id rev]} :params
                       :as req}]
  (let [{api-addr :api-addr} (:web req)]
    @(http/delete (format "http://%s/files/%s/%s"
                          api-addr id (make-model-sid id rev ssid))
                  {:body    ""
                   :timeout 1000
                   :as      :text})
    {:status 200}))

(defn read-log [{{storage :storage} :web
                 {:keys [id ssid]} :params :as req}]
  (let [model-id (Integer/valueOf ^String id)]
    (if-let [result (slog/read-log storage model-id ssid)]
      (render-profile-page req model-id
                           :in-params (-> result
                                          :in_params
                                          (json/parse-string true)
                                          malt-params->form-values)
                           :out-params (:out_params result)
                           :log-session-id ssid
                           :flash {:success "Loaded from log."})
      (render-profile-page req model-id
                           :log-session-id ssid
                           :flash {:error "No such log entry."}))))