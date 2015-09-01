(ns malt-admin.view.errors
  (:require [malt-admin.view.utils :as u :refer [h]]
            [malt-admin.view.layout :refer [layout-body]]
            [malt-admin.form.auth :as form]))

(defn e401 [ctx]
  (layout-body
    ctx
    [:div {:class "alert alert-danger text-center"}
     [:h1
      [:span "Invalid credentials"]]]))

(defn e403 [ctx]
  (layout-body
    ctx
    [:div {:class "alert alert-danger text-center"}
     [:h1
      [:span "Access denied"]]]))

(defn e404 [ctx]
  (layout-body
    ctx
    [:div {:class "alert alert-danger text-center"}
     [:h1
      [:span "Page not found"]]]))

(defn e500 [ctx]
  (layout-body
    ctx
    [:div {:class "alert alert-danger text-center"}
     [:h1
      [:span "Internal error"]]]))