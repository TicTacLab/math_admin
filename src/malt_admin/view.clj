(ns malt-admin.view
  (:require [selmer.parser :as selmer]
            [selmer.filters :as filters]
            [formative.core :as f]
            [hiccup.core :as h]
            [clojure.pprint :refer [pprint]]
            [ring.util.response :as res]
            [malt-admin.config :as c])
  (:import [java.text SimpleDateFormat]
           [java.util TimeZone]))


(defn- format-timestamp [pattern timestamp-sec]
  (let [ timestamp (* (Long. timestamp-sec) 1000)]
       (.format (doto (SimpleDateFormat. pattern)
             (.setTimeZone (TimeZone/getTimeZone "GMT")))
           timestamp)))

(defn- pprint-str [value]
  (with-out-str (pprint value)))

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
                          :admin?     (get-in req [:session :is-admin])
                          :uri        (:uri req)
                          :env        @c/config
                          :flash      flash}
         context (merge context default-context)]
    (selmer/render-file (str template-name ".html") context
                        {:tag-open \[
                         :tag-close \]
                         :filter-open \[
                         :filter-close \]})))

(defn render-error [req code]
  (-> (format "errors/%d" code)
      (render req {})
      res/response
      (res/status code)))