(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :directories #{#_"target"}
  :dependencies '[[org.clojure/clojure "1.6.0"]
                  [javax.servlet/javax.servlet-api "3.1.0"]
                  [com.stuartsierra/component "0.2.2"]
                  [environ "1.0.0"]
                  [org.slf4j/slf4j-api "1.7.7"]
                  [ch.qos.logback/logback-core "1.1.2"]
                  [ch.qos.logback/logback-classic "1.1.2"]
                  [org.clojure/tools.logging "0.3.1"]
                  [clojurewerkz/cassaforte "2.0.0" :exclusions [com.google.guava/guava]]
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
                  [ring-webjars "0.1.0" :exclusions [org.slf4j/slf4j-nop]]
                  [org.webjars/bootstrap "3.3.2"]
                  [org.webjars/jquery "2.1.3"]
                  [org.webjars/metroui "2.0.23"]
                  [clj-webdriver "0.6.1" :exclusions [com.google.guava/guava org.seleniumhq.selenium/selenium-server]]
                  [org.seleniumhq.selenium/selenium-server "2.44.0" :exclusions [com.google.guava/guava org.yaml/snakeyaml]]
                  [com.aphyr/prism "0.1.1"]
                  [org.cassandraunit/cassandra-unit "2.0.2.2" :exclusions [org.slf4j/slf4j-log4j12]]
                  [com.toddfast.mutagen/mutagen-cassandra "0.4.0" :exclusions [org.jboss.netty/netty]]]
  :repositories {"nassau" {:url      "http://nassau/repository/internal"
                           :username "ci"
                           :password "ci1"}})

#_(extend-protocol boot.tmpdir/ITmpFile
  java.io.File
  (file [this] (.getAbsoluteFile this))
  (path [this] (.getAbsolutePath this)))

(task-options!
  pom  {:project 'malt_admin}
  aot  {:all true}
  uber {:as-jars true}
  jar  {:main 'malt_admin.main}
  push {:tag            true
        :ensure-release true
        :repo           "nassau"})

(deftask
  protoc
  []
  (let [tmp (tmp-dir!)]
    (fn [next-handler]
      (fn [fileset]
        (empty-dir! tmp)
        (doseq [f (->> fileset
                       (input-files)
                       (by-ext [".proto"])
                       (map boot.tmpdir/file))]
          (boot.util/info "Compiling protobuf file: %s\n" (.getName f))
          (let [res (clojure.java.shell/sh "/usr/bin/protoc"
                                           (str "--proto_path=" (.getParent f))
                                           (str "--java_out=" (.getPath tmp))
                                           (.getPath f))]
            (when-not (zero? (:exit res))
              (boot.util/fail "stdout: %s\n" (:out res))
              (boot.util/fail "stderr: %s\n" (:err res)))))
        (-> fileset
            (add-source (clojure.java.io/file tmp))
            (commit!)
            next-handler)))))

(deftask
  release
  [v version VER    str    "The new project version"]
  (comp (pom :version version)
        (protoc)
        (javac)

        (aot)
        (uber)
        (jar)
        (push :file-regex #{(re-pattern (format "malt_admin-%s.jar$" version))})))