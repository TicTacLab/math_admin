(ns malt-admin.audit
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log])
  (:import (java.util Date)))

(defn message [req action subject]
  (json/generate-string {:who     (get-in req [:session :login])
                         :ip      (get-in req [:remote-addr])
                         :when    (.getTime (Date.))
                         :action  action
                         :subject subject}))

(defn info [req action subject]
  (log/info (message req action subject)))

(defn warn [req action subject]
  (log/warn (message req action subject)))
