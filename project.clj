(defproject fogus/bacwn "0.3.0-SNAPSHOT"
  :description "A Datalog for Clojure"
  :url "http://www.fogus.me/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :plugins [[lein-cljsbuild "0.3.2"]
            [lein-marginalia "0.7.1"]]
  :extra-classpath-dirs ["checkouts/clojurescript/src/clj"
                         "checkouts/clojurescript/src/cljs"]
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :cljsbuild {:test-commands {"unit-tests" ["phantomjs" "runner.js" "target/unit-test.js"]}
              :builds
              {:test {:source-paths ["src/clojure" "src/clojurescript" "test-cljs"],
                      :incremental false,
                      :compiler {:pretty-print true,
                                 :output-to "target/unit-test.js",
                                 :optimizations :whitespace}}}})

