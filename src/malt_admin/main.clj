(ns malt-admin.main
  (:require [environ.core :as environ]
            [mait-admin.system :as s]
            [com.stuartsierra.component :as component])
  (:gen-class))

(defn -main [& args]
  (component/start (s/new-system environ/env)))
