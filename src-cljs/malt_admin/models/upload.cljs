(ns ^:figwheel-always malt-admin.models.upload
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)


(defn load-file-step []
  [:div
   [:h3 "Load Excel File"]
   [:form {:id    "load-file-form"
           :class "form-horizontal"}
    [:div {:class "form-group"}
     [:label {:class "col-sm-2 control-label"
              :for   "model-file"}
      "Excel File*"]
     [:div {:class "col-sm-10"}
      [:input {:id     "model-file"
               :class  "form-control"
               :type   "file"
               :accept ".xls, .xlsx"}]
      [:p {:class "help-block"} "Maximum size is 50 megabytes."]]]]])

(defn navigation []
  [:div {:class "row navigation"}
   [:div {:class "col-md-3"}
    [:span {:class "navigation-active"} "Load Excel File"]
    [:span {:class "pull-right"} "→"]]
   [:div {:class "col-md-3"}
    [:span {:class "navigation-default"} "Select In Cells"]
    [:span {:class "pull-right"} "→"]]
   [:div {:class "col-md-3"}
    [:span {:class "navigation-default"} "Select Out Cells"]
    [:span {:class "pull-right"} "→"]]
   [:div {:class "col-md-3"}
    [:span {:class "navigation-default"} "Review and Upload"]
    [:span {:class "pull-right"} "→"]]])


(defn load-excel-file [e]
  ;; TODO check if file was choosed and prettify errors
  (let [file (-> (.getElementById js/document "model-file")
                 (.-files)
                 (aget 0))]
    (if (> (.-size file) 52428800)
      (js/console.log "File is too big!!")
      (go
        (let [res (<! (http/post "/models/upload-wizard/load-file"
                                 {:multipart-params [["file" file]]}))]
          (println res))))))

(defn step-controls []
  [:div
   [:button {:id "step-cancel"
             :class "btn btn-default"
             :type "button"}
    "Cancel"]
   [:button {:id "step-next"
             :class "btn btn-primary"
             :type "button"
             :on-click load-excel-file}
    "Next"]])

(r/render-component [navigation]
                    (.getElementById js/document "navigation-container"))
(r/render-component [load-file-step]
                    (.getElementById js/document "load-file-step"))
(r/render-component [step-controls]
                    (.getElementById js/document "step-controls"))