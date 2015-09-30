(defproject malt_admin "0.1.3-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.betinvest/noilly "0.1.4"]
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 [com.stuartsierra/component "0.2.3"]
                 [org.slf4j/slf4j-api "1.7.12"]
                 [ch.qos.logback/logback-core "1.1.3"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clojurewerkz/cassaforte "2.0.2" :exclusions [com.google.guava/guava]]
                 [clojurewerkz/scrypt "1.2.0"]
                 [compojure "1.4.0"]
                 [selmer "0.9.1"]
                 [ring "1.4.0"]
                 [cheshire "5.5.0"]
                 [http-kit "2.1.19"]
                 [prismatic/schema "1.0.1"]
                 [formative "0.8.8" :exclusions [org.clojure/clojurescript
                                                 clj-time]]
                 [hiccup "1.0.5"]
                 [com.taoensso/nippy "2.9.0"]
                 [ring-webjars "0.1.1" :exclusions [org.slf4j/slf4j-nop]]
                 [org.webjars/bootstrap "3.3.4"]
                 [org.webjars/jquery "2.1.3"]
                 [org.webjars/metroui "2.0.23"]
                 [org.clojure/test.check "0.8.1"]
                 [com.betinvest/zabbix-clojure-agent "0.1.8"]
                 [clj-webdriver "0.7.2" :exclusions [com.google.guava/guava org.seleniumhq.selenium/selenium-server]]
                 [org.seleniumhq.selenium/selenium-server "2.47.1" :exclusions [com.google.guava/guava org.yaml/snakeyaml]]
                 [com.aphyr/prism "0.1.3"]
                 [org.cassandraunit/cassandra-unit "2.1.9.2" :exclusions [org.slf4j/slf4j-log4j12]]

                 [org.owasp.passfault/passfault-core "0.7"]

                 [org.clojure/clojurescript "1.7.48"]
                 [prismatic/dommy "1.1.0"]
                 [reagent "0.5.1"]

                 ;Sanitizers
                 [org.owasp.encoder/encoder "1.2"]]
  :repl-options {:timeout 120000
                 :init-ns user}
  :main malt-admin.main
  :plugins [[com.aphyr/prism "0.1.1"]
            [theladders/lein-uberjar-deploy "1.0.0"]
            [lein-cljsbuild "1.1.0"]
            [lein-figwheel "0.4.0"]]
  :aliases {"autotest" ["with-profile" "test" "prism"]}
  :repositories ^:replace [["snapshots" {:url "http://52.28.244.218:8080/repository/snapshots"
                                         :username :env
                                         :password :env}]
                           ["releases" {:url "http://52.28.244.218:8080/repository/internal"
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
             :dev        {:source-paths ["dev"]
                          :global-vars {*warn-on-reflection* false}
                          :plugins [[lein-figwheel "0.4.0"]]
                          :dependencies [[ns-tracker "0.3.0"]
                                         [aprint "0.1.3"]
                                         [http-kit.fake "0.2.2"]
                                         [criterium "0.4.3"]
                                         [im.chit/vinyasa "0.4.1"]
                                         [org.clojure/tools.trace "0.7.8"]]
                          :injections [(require '[vinyasa.inject :as inject])
                                       (require 'aprint.core)
                                       (require 'clojure.pprint)
                                       (require 'clojure.tools.trace)
                                       (require 'criterium.core)
                                       (inject/in clojure.core >
                                                  [aprint.core aprint]
                                                  [clojure.pprint pprint]
                                                  [clojure.tools.trace trace]
                                                  [criterium.core bench])]}
             :test       {:dependencies [[http-kit.fake "0.2.2"]]}}
  :cljsbuild {:builds [{:id "dev"
                        :figwheel true
                        :source-paths ["src-cljs"]
                        :compiler {
                                   :optimizations :none
                                   :asset-path   "/js/compiled"
                                   :output-dir   "resources/public/js/compiled"
                                   :output-to    "resources/public/js/compiled/main.js"}}
                       {:id "min"
                        :optimizations :advanced
                        :compiler {:source-paths ["src-cljs"]
                                   :output-dir   "resources/public/js/compiled"
                                   :output-to    "resources/public/js/compiled/main.js"}}]}
  :figwheel {:nrepl-port 7999})
