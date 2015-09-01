(ns malt-admin.view.auth
  (:require [malt-admin.view.utils :as u :refer [h]]
            [malt-admin.view.layout :refer [layout-body]]
            [malt-admin.form.auth :as form]))

(defn index [ctx]
  (layout-body
    ctx
    (list [:h1 "Sign in"]
          (u/render-form form/signin))))