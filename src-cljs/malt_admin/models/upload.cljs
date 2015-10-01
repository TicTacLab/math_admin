(ns ^:figwheel-always malt-admin.models.upload
  (:require [malt-admin.models.upload.load-excel-file :as load-excel-file]
            [malt-admin.models.upload.select-in-cells :as select-in-cells]
            [reagent.core :as r]))

(enable-console-print!)

(def steps
  [load-excel-file/step
   select-in-cells/step])

(defn first? [curr-step]
  (zero? curr-step))

(defn next-step [curr-step total-steps]
  (if (< (inc curr-step) total-steps)
    (inc curr-step)
    curr-step))

(defn prev-step [curr-step]
  (if (first? curr-step)
    curr-step
    (dec curr-step)))

(defn navigation [curr-state steps]
  [:div {:class "row navigation"}
   (for [[step sep] (->> (assoc-in steps [@curr-state :active] true)
                         (interpose "→")
                         (partition-all 2))]
     [:div {:class "col-md-3"
            :key (:name step)}
      [:span {:class (if (:active step) "navigation-active" "navigation-default")}
       (:name step)]
      (when sep
        [:span {:class "pull-right"} sep])])])

(defn render-step [curr-state steps states]
  (let [step (get steps @curr-state)
        state (get states @curr-state)
        body (:body-comp step)
        valid-fn (:valid-fn step)
        next-fn (:next-fn step)]
    (fn []
      [:div {:class "modal-body"}
       [:div {:class "container-fluid"}
        (navigation curr-state steps)
        [:div
         [:h3 (:name step)]
         [(body state)]]]
       [:div {:class "modal-footer"}
        [:div
         [:button {:id    "step-cancel"
                   :class "btn btn-default"
                   :type  "button"}
          "Cancel"]
         (when (not (first? @curr-state))
           [:button {:id    "step-back"
                     :class "btn btn-default"
                     :type  "button"
                     :on-click (fn [e] (swap! curr-state prev-step))}
            "Back"])
         [:button {:id       "step-next"
                   :class    (if (valid-fn @state) "btn btn-primary" "btn btn-primary disabled")
                   :type     "button"
                   :disabled (not (valid-fn @state))
                   :on-click (fn [e]
                               (when (valid-fn @state)
                                 (next-fn state e)
                                 (swap! curr-state next-step (count steps))))}
          "Next"]]]])))


(defn render-wizard [steps]
  (let [curr-state (r/atom 0)
        states (mapv #(r/atom ((:state-fn %))) steps)]
    (fn []
      [(render-step curr-state steps states)])))

(r/render-component [(render-wizard steps)]
                    (.getElementById js/document "step"))