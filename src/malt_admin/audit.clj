(ns malt-admin.audit
  (:require [cheshire.core :as json]
            [clojure.tools.logging :as log])
  (:import (java.util Date)))

(defn audit [req action subject]
  (log/info (json/generate-string {:who     (get-in req [:session :login])
                                   :when    (.getTime (Date.))
                                   :action  action
                                   :subject subject})))
