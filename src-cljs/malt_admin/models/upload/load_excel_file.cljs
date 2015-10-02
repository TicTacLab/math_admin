(ns malt-admin.models.upload.load-excel-file
  (:require [cljs-http.client :as http])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defn check-file-existence [step-state event]
  (let [file (-> (.getElementById js/document "model-file")
                   (.-files)
                   (aget 0))]
    (js/console.log file)
    (swap! step-state assoc :file file)))

(defn load-excel-file [step-state event]
  (when-let [file (-> (.getElementById js/document "model-file")
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
            (do

              (println (<! (http/get "/models/upload-wizard/get-sheets-names"))))))))))

(def step
  {:name      "Load Excel File"
   :state-fn  (fn [] {:errors #{}
                      :file   nil})
   :body-comp (fn [state]
                (fn []
                  [:form {:id    "load-file-form"
                          :class "form-horizontal"}
                   [:div {:class (if (seq (:errors @state)) "form-group has-error" "form-group")}
                    [:label {:class "col-sm-2 control-label"
                             :for   "model-file"}
                     "Excel File*"]
                    [:div {:class "col-sm-10"}
                     [:input {:id        "model-file"
                              :class     "form-control"
                              :type      "file"
                              :accept    ".xls, .xlsx"
                              :on-change (partial check-file-existence state)
                              :value     (:file @state)
                              }]
                     [:p {:class "help-block"} "Maximum size is 50 megabytes."]
                     (for [e (:errors @state)]
                       [:p {:class "help-block", :key e} e])]]]))

   :next-fn   load-excel-file
   :valid-fn  :file})