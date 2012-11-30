(defproject fogus/bacwn "0.3.0-SNAPSHOT"
  :description "A Datalog for Clojure"
  :url "http://www.fogus.me/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-swank "1.4.4"]
            [lein-cljsbuild "0.2.9"]
            [lein-marginalia "0.7.1"]
            [lein-multi "1.1.0"]]
  :multi-deps {"1.2" [[org.clojure/clojure "1.2.0"]]
               "1.3" [[org.clojure/clojure "1.3.0"]]
               "1.4" [[org.clojure/clojure "1.4.0"]]
               "1.5" [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]]}
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :extra-classpath-dirs ["checkouts/clojurescript/src/clj"
                         "checkouts/clojurescript/src/cljs"]
  :source-paths ["src/clojure"]
  :test-paths ["test/clojure"]
  :cljsbuild {:builds [{:source-path "src/clojurescript"
                        :compiler {:output-to "resources/js/bacwn.none.js"
                                   :output-dir "resources/js/"
                                   :optimizations :none}}]}
  :dependencies [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]])

