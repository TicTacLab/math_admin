(ns malt-admin.calc-formatting
  (:use clojure.test)
  (:require [malt-admin.controller.models :as m]))

(deftest formatting-test
  (let [test-in {:data [{:coef 1.53, :id 58, :m_code "MATCH_TOTAL_GOAL",
                         :market "Match Totals  ", :mgp_code "MATCH", :mgp_weight 1,
                         :mn_code "TOTAL", :mn_weight 11, :o_code "OVER", :outcome "Over",
                         :param 2.0, :param2 0.0, :timer 0}
                        {:coef 2.35, :id 59, :m_code "MATCH_TOTAL_GOAL",
                         :market "Match Totals  ", :mgp_code "MATCH", :mgp_weight 1,
                         :mn_code "TOTAL", :mn_weight 11, :o_code "UNDER", :outcome "Under",
                         :param 2.0, :param2 0.0, :timer 0}
                        {:coef 1.33, :id 62, :m_code "MATCH_TOTAL_GOAL",
                         :market "Match Totals  ", :mgp_code "MATCH", :mgp_weight 1,
                         :mn_code "TOTAL", :mn_weight 11, :o_code "OVER", :outcome "Over",
                         :param 1.5, :param2 0.0, :timer 0}
                        {:coef 3.05, :id 63, :m_code "MATCH_TOTAL_GOAL",
                         :market "Match Totals  ", :mgp_code "MATCH", :mgp_weight 1,
                         :mn_code "TOTAL", :mn_weight 11, :o_code "UNDER", :outcome "Under",
                         :param 1.5, :param2 0.0, :timer 0}
                        {:coef 0.99, :id 705, :m_code "EXTRA_TIME_1_BETTING",
                         :market "1x2 Extra Time 1", :mgp_code "ET1", :mgp_weight 62,
                         :mn_code "WIN", :mn_weight 1, :o_code "HOME", :outcome "1.0",
                         :param 999999.0, :param2 0.0, :timer 0}
                        {:coef 0.99, :id 706, :m_code "EXTRA_TIME_1_BETTING",
                         :market "1x2 Extra Time 1", :mgp_code "ET1", :mgp_weight 62,
                         :mn_code "WIN", :mn_weight 1, :o_code "DRAW", :outcome "X",
                         :param 999999.0, :param2 0.0, :timer 0}
                        {:coef 0.99, :id 707, :m_code "EXTRA_TIME_1_BETTING",
                         :market "1x2 Extra Time 1", :mgp_code "ET1", :mgp_weight 62,
                         :mn_code "WIN", :mn_weight 1, :o_code "AWAY", :outcome "2.0",
                         :param 999999.0, :param2 0.0, :timer 0}
                        {:coef 0.99, :id 719, :m_code "EXTRA_TIME_1_TOTAL_GOAL",
                         :market "Extra Time 1 Totals", :mgp_code "ET1", :mgp_weight 62,
                         :mn_code "TOTAL", :mn_weight 11, :o_code "OVER", :outcome "Over",
                         :param 1.0, :param2 0.0, :timer 0}
                        {:coef 0.99, :id 720, :m_code "EXTRA_TIME_1_TOTAL_GOAL",
                         :market "Extra Time 1 Totals", :mgp_code "ET1", :mgp_weight 62,
                         :mn_code "TOTAL", :mn_weight 11, :o_code "UNDER", :outcome "Under",
                         :param 1.0, :param2 0.0, :timer 0}],
                 :timer 777, :type :outcomes}
        test-out [[:type :outcomes]
                  [:data
                   {"ET1" '([["WIN" 999999.0 0.0]
                             '('({:coef 0.99, :id 705, :m_code "EXTRA_TIME_1_BETTING",
                                  :market "1x2 Extra Time 1", :mgp_code "ET1", :mgp_weight 62,
                                  :mn_code "WIN", :mn_weight 1, :o_code "HOME", :outcome "1.0",
                                  :param 999999.0, :param2 0.0, :timer 0}
                                  {:coef 0.99, :id 706, :m_code "EXTRA_TIME_1_BETTING",
                                   :market "1x2 Extra Time 1", :mgp_code "ET1", :mgp_weight 62,
                                   :mn_code "WIN", :mn_weight 1, :o_code "DRAW", :outcome "X",
                                   :param 999999.0, :param2 0.0, :timer 0}
                                  {:coef 0.99, :id 707, :m_code "EXTRA_TIME_1_BETTING",
                                   :market "1x2 Extra Time 1", :mgp_code "ET1", :mgp_weight 62,
                                   :mn_code "WIN", :mn_weight 1, :o_code "AWAY", :outcome "2.0",
                                   :param 999999.0, :param2 0.0,
                                   :timer 0}))]
                             [["TOTAL" 1.0 0.0]
                              '('({:coef 0.99, :id 719, :m_code "EXTRA_TIME_1_TOTAL_GOAL",
                                   :market "Extra Time 1 Totals", :mgp_code "ET1", :mgp_weight 62,
                                   :mn_code "TOTAL", :mn_weight 11, :o_code "OVER", :outcome "Over",
                                   :param 1.0, :param2 0.0, :timer 0}
                                   {:coef 0.99, :id 720, :m_code "EXTRA_TIME_1_TOTAL_GOAL",
                                    :market "Extra Time 1 Totals", :mgp_code "ET1", :mgp_weight 62,
                                    :mn_code "TOTAL", :mn_weight 11, :o_code "UNDER",
                                    :outcome "Under", :param 1.0, :param2 0.0,
                                    :timer 0}))]),
                    "MATCH" '([["TOTAL" 1.5 0.0]
                               '('({:coef 1.33, :id 62, :m_code "MATCH_TOTAL_GOAL",
                                    :market "Match Totals  ", :mgp_code "MATCH", :mgp_weight 1,
                                    :mn_code "TOTAL", :mn_weight 11, :o_code "OVER",
                                    :outcome "Over", :param 1.5, :param2 0.0,
                                    :timer 0}
                                    {:coef 3.05, :id 63, :m_code "MATCH_TOTAL_GOAL",
                                     :market "Match Totals  ", :mgp_code "MATCH", :mgp_weight 1,
                                     :mn_code "TOTAL", :mn_weight 11, :o_code "UNDER",
                                     :outcome "Under", :param 1.5, :param2 0.0,
                                     :timer 0}))]
                               [["TOTAL" 2.0 0.0]
                                '('({:coef 1.53, :id 58, :m_code "MATCH_TOTAL_GOAL",
                                     :market "Match Totals  ", :mgp_code "MATCH", :mgp_weight 1,
                                     :mn_code "TOTAL", :mn_weight 11, :o_code "OVER",
                                     :outcome "Over", :param 2.0, :param2 0.0,
                                     :timer 0}
                                     {:coef 2.35, :id 59, :m_code "MATCH_TOTAL_GOAL",
                                      :market "Match Totals  ", :mgp_code "MATCH", :mgp_weight 1,
                                      :mn_code "TOTAL", :mn_weight 11, :o_code "UNDER",
                                      :outcome "Under", :param 2.0, :param2 0.0,
                                      :timer 0}))])}]
                  [:timer 777]]]
    (is test-out
        (->> test-in
             m/format-calc-result
            (into [])))))