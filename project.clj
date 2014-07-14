(defproject clojure-cassandra-demo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cc.qbits/alia "2.0.0-rc4"]
                 [compojure "1.1.8"]
                 [ring "1.3.0"]
                 [ring/ring-json "0.3.1"]
                 [me.raynes/least "0.1.3"]
                 [log4j/log4j "1.2.17"]
                 [org.slf4j/slf4j-api "1.7.5"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [org.clojure/tools.logging "0.2.6"]
                 [clj-time "0.7.0"]
                 [hiccup "1.0.5"]]
  :plugins  [[lein-cljsbuild "1.0.3"]
             [lein-ring "0.8.11"]]
  :ring {:handler clojure-cassandra-demo.core/app
         :init clojure-cassandra-demo.core/startup
         :auto-reload true}
  :source-paths  ["src/clj"]
  :main ^:skip-aot clojure-cassandra-demo.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
