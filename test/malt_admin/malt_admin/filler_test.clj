(ns malt-admin.malt-admin.filler-test
  (:use clojure.test)
  (:require [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [malt-admin.test-helper :as t]
            [malt-admin.config :as c]
            [malt-admin.storage.cache-q :as cache-q]
            [clojurewerkz.cassaforte.cql :as cql]
            [taoensso.nippy :as nippy]
            [org.httpkit.server :as http-kit]))

(def in-params-generator (gen/vector
                               (gen/hash-map :model_id gen/s-pos-int
                                             :params (gen/not-empty
                                                       (gen/vector
                                                         (gen/hash-map
                                                           :id gen/s-pos-int
                                                           :value (gen/one-of [gen/int gen/string-alphanumeric]))))
                                             )))


(def req-counter (atom 0))
(let [server (http-kit/run-server (fn [_] {(swap! req-counter inc)})
                                  {:port 55555
                                   :host "localhost"
                                   :max-body 52428800 ;; 50Mb
                                   :join? false})]
  (when server (server)))


(t/with-system [sys (t/test-system @c/config)]
  (tc/quick-check
    10
    (prop/for-all [in-params in-params-generator
                   rev (gen/not-empty gen/string-alphanumeric)]
      (cql/truncate (get-in sys [:storage :conn]) "cache_q")
      (cache-q/insert-in-params! (:storage sys) rev (map #(update-in % [:params] nippy/freeze) in-params))
      (Thread/sleep 20000)
      (= (count in-params)
         (count (cql/select (get-in sys [:storage :conn]) "cache"))))))
