(ns malt-admin.hand-profiler
  (:require [cheshire.core :as json]
            [criterium.core :as cri]
            [org.httpkit.client :as http]))

(def params  {:id 25
                  :ssid "CAFEBABE"
                  :params
                  [{:id 1, :value 78}
                   {:id 2, :value 44}
                   {:id 3, :value "A"}
                   {:id 4, :value 2}
                   {:id 5, :value 0}
                   {:id 6, :value 0}
                   {:id 7, :value 0}
                   {:id 8, :value 0}
                   {:id 9, :value 0}
                   {:id 10, :value 1.987878}
                   {:id 11, :value 0}
                   {:id 12, :value 0}
                   {:id 13, :value 0}
                   {:id 14, :value 0}
                   {:id 15, :value 1.4342}
                   {:id 16, :value 0}
                   {:id 17, :value 0}
                   {:id 18, :value 0}
                   {:id 19, :value 0}
                   {:id 20, :value 0}
                   {:id 21, :value 0}
                   {:id 22, :value 0}
                   {:id 23, :value 0}
                   {:id 24, :value 0}
                   {:id 25, :value 0}
                   {:id 26, :value 0}
                   {:id 27, :value 0}
                   {:id 28, :value 0}
                   {:id 29, :value 0}
                   {:id 30, :value 1}
                   {:id 31, :value 5}
                   {:id 32, :value 0}]})

(defn pack-params [model-id session-id params]
  (-> params
      (assoc :ssid session-id
             :id model-id)

      (json/generate-string)))

(defn profile
  [url model-id session-id params]
  @(http/post url {:body    (pack-params model-id session-id params)
                   :headers {"Content-type" "text/plain"}
                   :timeout 60000
                   :as      :byte-array})
  nil)

(defn profile-new [host port model-id session-id params]
  (-> (format "http://%s:%s/model/calc/%s/binary" host port session-id)
      (profile model-id session-id params)))

(defn profile-old [host port model-id session-id params]
  (-> (format "http://%s:%s/rest/calc" host port)
      (profile model-id session-id params)))