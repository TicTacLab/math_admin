(ns ^:figwheel-always malt-admin.models.upload
  (:require [reagent.core :as r]
            [cljs-http.client :as http]
            [cljs.core.async :as a])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defn load-excel-file [step-state event]
  ;; TODO check if file was choosed and prettify errors
  (let [file (-> (.getElementById js/document "model-file")
                 (.-files)
                 (aget 0))]
    (cond
      (> (.-size file) 52428800)
      (swap! step-state update :errors conj "File is too big")

      :else
      (go
        (let [res (<! (http/post "/models/upload-wizard/load-file"
                                 {:multipart-params [["file" file]]}))]
          (if-not (:success res)
            (swap! step-state update :errors into (get-in res [:body :errors]))
            (println res)))))))

(defn check-file-existence [step-state event]
  (swap! step-state assoc :valid? (-> (.getElementById js/document "model-file")
                                      (.-files)
                                      (aget 0)
                                      (boolean)))
  (println @step-state)
  :ok)

(defn load-file-step []
  (let [step-state (r/atom {:errors #{}
                            :valid? false})]
    (fn []
      [:div {:class "modal-body"}
       [:div {:class "container-fluid"}
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
          [:span {:class "pull-right"} "→"]]]
        [:div
         [:h3 "Load Excel File"]
         [:form {:id    "load-file-form"
                 :class "form-horizontal"}
          [:div {:class (if (seq (:errors @step-state)) "form-group has-error" "form-group")}
           [:label {:class "col-sm-2 control-label"
                    :for   "model-file"}
            "Excel File*"]
           [:div {:class "col-sm-10"}
            [:input {:id     "model-file"
                     :class  "form-control"
                     :type   "file"
                     :accept ".xls, .xlsx"
                     :on-change (partial check-file-existence step-state)}]
            [:p {:class "help-block"} "Maximum size is 50 megabytes."]
            (for [e (:errors @step-state)]
              [:p {:class "help-block", :key e} e])]]]]]
       [:div {:class "modal-footer"}
        [:div
         [:button {:id    "step-cancel"
                   :class "btn btn-default"
                   :type  "button"}
          "Cancel"]
         [:button {:id       "step-next"
                   :class    (if (:valid? @step-state) "btn btn-primary" "btn btn-primary disabled")
                   :type     "button"
                   :disabled (not (:valid? @step-state))
                   :on-click (partial load-excel-file step-state)}
          "Next"]]]])))

(r/render-component [load-file-step]
                    (.getElementById js/document "step"))