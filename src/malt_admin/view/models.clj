(ns malt-admin.view.models
  (:require [malt-admin.view.utils :as u :refer [h]]
            [malt-admin.view.layout :refer [layout-body]]
            [malt-admin.form.model :as form]))

(defn index [{:keys [admin? models] :as ctx}]
  (layout-body
    ctx
    (list
      (when admin?
        [:a {:href "/models/upload", :class "btn btn-primary btn-lg"} "Upload New"])
      [:table {:class "table table-hover"}
       [:tbody
        [:tr
         [:th "ID"]
         [:th "Name"]
         [:th "File Name"]
         [:th "In Sheet Name"]
         [:th "Out Sheet Name"]
         [:th "Last Modified"]
         [:th "Actions"]]
        (for [m models]
          (let [id (:id m)]
            [:tr.model {:data-rev (h (:rev m))
                        :data-id  (h id)}
             [:td.model-id (h id)]
             [:td.model-name (h (:name m))]
             [:td.model-file-name (h (:file_name m))]
             [:td.model-in-sheet-name (h (:in_sheet_name m))]
             [:td.model-out-sheet-name (h (:out_sheet_name m))]
             [:td.model-last-modified (h (:last_modified m))]
             [:td
              [:form.form-inline {:action (h "/models/" id), :method "POST"}
               [:div.btn-group
                [:a.btn.btn-primary {:href (h "/models/" id "/" (:rev m) "/profile")}
                 "Profile"]
                (when admin?
                  (list
                    [:a.btn.btn-default.confirmable {:href (h "/models/" id "/download")}
                     "Download"]
                    [:a.btn.btn-default {:href (h "/models/" id "/edit")}
                     "Replace"]
                    [:input {:type "hidden", :name "_method", :value "DELETE"}]
                    [:button.btn.btn-danger.confirmable {:type "submit"}
                     "Delete"]))]]]]))]]
      [:script "$('.confirmable').click(function() { return confirm('Are you sure?') })"])))

(defn edit [{:keys [edit-form] :as ctx}]
  (layout-body
    ctx
    (list
      [:h1 "Replace Model"]
      (u/render-form edit-form)
      [:script "$('#field-submit').click (function () {return confirm ('Are you sure?')})"])))