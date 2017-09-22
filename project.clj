(defproject invetica/boot-from-lein "1.2.3-SNAPSHOT"
  :description "This is the description"
  :url "https://example.com/boot-from-lein"
  :dependencies [[org.clojure/clojure "1.9.0-beta1"]]
  :aliases {:not-supported ["sorry"]}
  :main com.example.main
  :repl-options {:init-ns user}
  :uberjar-name "example-standalone.jar"
  :profiles
  {:dev {:dependencies [[org.clojure/test.check "0.10.0-alpha2"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
   :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :uberjar {:aot :all
             :omit-source true}})
