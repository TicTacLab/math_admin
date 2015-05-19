(defproject malt_admin "0.1.3-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [environ "1.0.0"]
                 [org.slf4j/slf4j-api "1.7.7"]
                 [ch.qos.logback/logback-core "1.1.2"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clojurewerkz/cassaforte "2.0.1" :exclusions [com.google.guava/guava]]
                 [clojurewerkz/scrypt "1.2.0"]
                 [compojure "1.2.0"]
                 [selmer "0.7.2"]
                 [ring "1.3.2"]
                 [cheshire "5.3.1"]
                 [http-kit "2.1.16"]
                 [org.clojure/tools.trace "0.7.8"]
                 [prismatic/schema "0.3.6"]
                 [org.flatland/protobuf "0.8.1"]
                 [com.google.protobuf/protobuf-java "2.5.0"]
                 [formative "0.8.8" :exclusions [org.clojure/clojurescript
                                                 clj-time]]
                 [hiccup "1.0.5"]
                 [criterium "0.4.3"]
                 [ring-webjars "0.1.0" :exclusions [org.slf4j/slf4j-nop]]
                 [org.webjars/bootstrap "3.3.2"]
                 [org.webjars/jquery "2.1.3"]
                 [org.webjars/metroui "2.0.23"]
                 [clj-webdriver "0.6.1" :exclusions [com.google.guava/guava org.seleniumhq.selenium/selenium-server]]
                 [org.seleniumhq.selenium/selenium-server "2.44.0" :exclusions [com.google.guava/guava org.yaml/snakeyaml]]
                 [com.aphyr/prism "0.1.1"]
                 [org.cassandraunit/cassandra-unit "2.0.2.2" :exclusions [org.slf4j/slf4j-log4j12]]]
  :repl-options {:timeout 120000
                 :init-ns user}
  :main malt-admin.main
  :plugins [[lein-environ "1.0.0"]
            [lein-protobuf "0.4.1"]
            [com.aphyr/prism "0.1.1"]
            [theladders/lein-uberjar-deploy "1.0.0"]]
  :aliases {"autotest" ["with-profile" "test" "prism"]}
  :repositories ^:replace [["snapshots" {:url "http://nassau.favorit/repository/snapshots"
                                         :username :env
                                         :password :env}]
                           ["releases" {:url "http://nassau.favorit/repository/internal"
                                        :username :env
                                        :password :env}]]
  :jvm-opts ["-Dlogback.configurationFile=logback.xml" "-Dwebdriver.chrome.driver=/usr/lib/chromium-browser/chromedriver"]
  :uberjar-name "malt-admin-standalone.jar"
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["uberjar-deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]
  :profiles {:production {:jvm-opts ["-Dlogback.configurationFile=logback.production.xml"]}
             :staging    {:jvm-opts ["-Dlogback.configurationFile=logback.production.xml"]}
             :dev        [:dev-env
                          {:source-paths ["dev"]
                           :dependencies [[ns-tracker "0.2.2"]
                                          [aprint "0.1.0"]
                                          [http-kit.fake "0.2.1"]]}]
             :test       [:test-env
                          {:dependencies [[http-kit.fake "0.2.1"]]}]})
