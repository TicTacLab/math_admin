(ns malt-admin.view
  (:require [selmer.parser :as selmer]
            [selmer.filters :as filters]
            [environ.core :as environ]
            [formative.core :as f]
            [hiccup.core :as h])
  (:import [java.text SimpleDateFormat]
           [java.util TimeZone]))


(defn- format-timestamp [pattern timestamp-sec]
  (let [ timestamp (* (Long. timestamp-sec) 1000)]
       (.format (doto (SimpleDateFormat. pattern)
             (.setTimeZone (TimeZone/getTimeZone "GMT")))
           timestamp)))

(defn- pprint-str [value]
  (let [s (java.io.StringWriter.)]
    (binding [*out* s]
      (clojure.pprint/pprint value)
      (.toString s))))

(filters/add-filter! :pprint pprint-str)

(filters/add-filter! :js-date #(format-timestamp "yyyy-MM-dd" %))
(filters/add-filter! :name name)


(filters/add-filter! :form #(-> %
                                (assoc :renderer :bootstrap3-stacked)
                                (f/render-form)
                                (h/html)))

(defn render [template-name req context]
  (let [{session-id :session-id
         flash      :flash} req
        default-context {:signed-in? (boolean session-id)
                         :uri (:uri req)
                         :env environ/env
                         :flash flash}
        context (merge context default-context)]
    (selmer/render-file (str template-name ".html") context
                        {:tag-open \[
                         :tag-close \]
                         :filter-open \[
                         :filter-close \]})))
