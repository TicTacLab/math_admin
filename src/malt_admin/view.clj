(ns malt-admin.view
  (:require [selmer.parser :as selmer]
            [selmer.filters :as filters]
            [formative.core :as f]
            [hiccup.core :as h]
            [clojure.pprint :refer [pprint]]
            [ring.util.response :as res]
            [malt-admin.config :as c]
            [clojure.string :as str])
  (:import [java.text SimpleDateFormat]
           [java.util TimeZone]
           (org.owasp.encoder Encode)
           (java.net URLEncoder)))

(defn h
  "Escapes html"
  [& strs]
  (Encode/forHtml (apply str strs)))

(defn u
  "Urlencode specified string"
  [o]
  (URLEncoder/encode (str o) "UTF-8"))

(intern 'selmer.filter-parser 'escape-html* h)

(defn- format-timestamp [pattern timestamp-sec]
  (let [timestamp (* (Long/valueOf ^String timestamp-sec) 1000)]
    (.format (doto (SimpleDateFormat. pattern)
               (.setTimeZone (TimeZone/getTimeZone "GMT")))
             timestamp)))

(defn- pprint-str [value]
  (with-out-str (pprint value)))

(defn valid-param? [param]
  (not= param "999999.0"))

(defn valid-extra-param? [param]
  (not (str/blank? (str param))))

(defn format-market [[mn-code param param2]]
  (cond
    (and (valid-param? param)
         (valid-extra-param? param2)) (str mn-code " " param " " param2)
    (and (valid-extra-param? param2)) (str mn-code " - " param2)
    (valid-param? param) (str mn-code " " param)
    :else mn-code))

(defn render-form [form]
  (-> form
      (assoc :renderer :bootstrap3-stacked)
      (f/render-form)
      (h/html)))

(filters/add-filter! :pprint pprint-str)
(filters/add-filter! :js-date #(format-timestamp "yyyy-MM-dd" %))
(filters/add-filter! :name name)
(filters/add-filter! :format-market format-market)
(filters/add-filter! :form render-form)
(filters/add-filter! :u u)

(defn render [template-name req context]
  (let [{session-id :session-id
         flash      :flash} req
         default-context {:signed-in?  (boolean session-id)
                          :admin?      (get-in req [:session :is-admin])
                          :uri         (:uri req)
                          :env         @c/config
                          :flash       flash}
        context (merge context default-context)]
    (selmer/render-file (str template-name ".html") context
                        {:tag-open \[
                         :tag-close \]
                         :filter-open \[
                         :filter-close \]})))

(defn render-with-success
  [template-name req success context]
  (render template-name (assoc req :flash {:success success}) context))

(defn render-with-error
  [template-name req error context]
  (render template-name (assoc req :flash {:error error}) context))

(defn render-error [req code]
  (-> (format "errors/%d" code)
      (render req {})
      res/response
      (res/status code)))