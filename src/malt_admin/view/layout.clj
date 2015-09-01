(ns malt-admin.view.layout
 (:require [malt-admin.view.utils :as u :refer [h]]))

(defn layout [ctx {:keys [head body]}]
  [:html
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "keywords", :content ""}]
    [:meta {:name "description", :content ""}]
    [:meta {:name "viewport", :content "width=device-width, initial-scale=1"}]
    [:link {:href "/assets/bootstrap/css/bootstrap.min.css", :rel "stylesheet"}]
    [:script {:src "/assets/jquery/jquery.min.js"}]
    [:script {:src "/assets/bootstrap/js/bootstrap.min.js"}]
    head
    [:title "Malt Admin"]]
   [:body
    [:nav {:class "navbar navbar-default"}
     [:div {:class "container"}
      [:div {:class "navbar-header"}
       [:button {:type "button", :class "navbar-toggle collapsed", :data-toggle "collapse", :data-target "#navbar", :aria-expanded "false", :aria-controls "navbar"}
        [:span {:class "sr-only"} "Toggle navigation"]
        [:span {:class "icon-bar"}]
        [:span {:class "icon-bar"}]
        [:span {:class "icon-bar"}]]]
      [:div {:id "navbar", :class "collapse navbar-collapse"}
       (when (:signed-in? ctx)
        [:ul {:class "nav navbar-nav"}
         (when (:admin? ctx)
          [:li
           [:a {:href "/settings"} "Settings"]]
          [:li
           [:a {:href "/users"} "Users"]])
         [:li
          [:a {:href "/models"} "Models"]]]
        [:form {:action "/auth", :method "post", :class "navbar-form navbar-right"}
         [:input {:type "hidden", :name "_method", :value "DELETE"}]
         [:button {:type "submit", :class "btn btn-default"} "Sign out"]])]]]
    [:div {:class "container-fluid"}
     [:div {:id "flash"}
      (when-let [msg (get-in ctx [:flash :success])]
       [:div {:class "alert alert-success alert-dismissible"}
        [:button {:type "button", :class "close", :data-dismiss "alert"} "×"]
        [:span {:id "flash-msg"}
         (h msg)]])

      (when-let [msg (get-in ctx [:flash :error])]
       [:div {:class "alert alert-danger alert-dismissible"}
        [:button {:type "button", :class "close", :data-dismiss "alert"} "×"]
        [:span {:id "flash-msg"}
         (h msg)]])]
     body
     [:script u/csrf-script]]]])
