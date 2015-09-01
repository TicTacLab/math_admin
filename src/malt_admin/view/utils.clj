(ns malt-admin.view.utils
  (:require [selmer.filters :as filters]
            [formative.core :as f]
            [hiccup.core :as h]
            [clojure.pprint :refer [pprint]]
            [ring.util.response :as res]
            [malt-admin.config :as c]
            [clojure.string :as str]
            [hiccup.page :as hiccup])
  (:import [java.text SimpleDateFormat]
           [java.util TimeZone]
           (org.owasp.encoder Encode)))

(defn h
  "Escapes html content:
     [:body (h UNTRUSTED)]"
  [& texts]
  (Encode/forHtmlContent (apply str texts)))

(defn- format-timestamp [pattern timestamp-sec]
  (let [ timestamp (* (Long/valueOf ^String timestamp-sec) 1000)]
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
      (f/render-form)))

(filters/add-filter! :pprint pprint-str)
(filters/add-filter! :js-date #(format-timestamp "yyyy-MM-dd" %))
(filters/add-filter! :name name)
(filters/add-filter! :format-market format-market)

(def csrf-script "(function() {
  var cookies = document.cookie;
  var matches = cookies.match(/csrf=([^;]*);?/);
  var token   = matches[1];
  $('form').each(function(i, rawForm) {
    var form = $(rawForm);
    if(form.attr('method').toLowerCase() === 'post') {
      var hidden = $('<input />');
      hidden.attr('type', 'hidden');
      hidden.attr('name', 'csrf');
      hidden.attr('value', token);
      form.append(hidden);
    }
  })
}());")

(defn render [template-fun req context]
  (let [{session-id :session-id
         flash      :flash} req
         default-context {:signed-in?  (boolean session-id)
                          :admin?      (get-in req [:session :is-admin])
                          :uri         (:uri req)
                          :env         @c/config
                          :flash       flash
                          :csrf-script csrf-script}
        context (merge context default-context)]
    (hiccup/html5 (template-fun context))))

(defn render-error [req code]
  (-> (format "errors/%d" code)
      (render req {})
      res/response
      (res/status code)))