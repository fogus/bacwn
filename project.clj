(defproject fogus/bacwn "0.3.0-SNAPSHOT"
  :description "A Datalog for Clojure"
  :url "http://www.fogus.me/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :plugins [[lein-cljsbuild "0.2.9"]
            [lein-marginalia "0.7.1"]]
  :profiles {:1.2 {:dependencies [[org.clojure/clojure "1.2.0"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}}
  :extra-classpath-dirs ["checkouts/clojurescript/src/clj"
                         "checkouts/clojurescript/src/cljs"]
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :cljsbuild {:builds [{:source-path "src/clojurescript"
                        :compiler {:output-to "resources/js/bacwn.none.js"
                                   :output-dir "resources/js/"
                                   :optimizations :none}}]})

