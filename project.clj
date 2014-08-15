(defproject fogus/bacwn "0.5.0-SNAPSHOT"
  :description "A Datalog for Clojure"
  :url "http://www.fogus.me/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2311"]
                 [com.cemerick/clojurescript.test "0.3.1"]
                 [org.clojure/core.async "0.1.319.0-6b1aca-alpha"]
                 [com.ydn/db "1.0.3"]]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-marginalia "0.7.1"]]

  :extra-classpath-dirs ["checkouts/clojurescript/src/clj"
                         "checkouts/clojurescript/src/cljs"]

  :source-paths ["src/clojure" "src/clojurescript"]

  :test-paths ["test/clojure"]

  :cljsbuild {:test-commands {"unit-tests" ["phantomjs" "runner.js" "target/unit-test.js"]}
              :builds
              [{:id "dev"
                :source-paths ["src" "dev"]
                :compiler {
                           :output-to "bacwn.js"
                           :output-dir "out"
                           :optimizations :none
                           :source-map true}}

               {:test {:source-paths ["src/clojure" "src/clojurescript" "test-cljs"],
                       :incremental false,
                       :compiler {:pretty-print true,
                                  :output-to "target/unit-test.js",
                                  :optimizations :whitespace
                                  :preamble ["db/ydn.db-iswu-crud.js"]
                                  :externs ["db/externs/ydn-db.js"]}}}]})
