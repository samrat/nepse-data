(defproject nepse-data "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/laser "1.1.1"]
                 [clj-http "0.7.3"]
                 [compojure "1.1.5"]
                 [ring/ring-json "0.2.0"]
                 [ring/ring-devel "1.1.8"]
                 [http-kit "2.1.4"]
                 [ring-cors "0.1.0"]
                 [clj-time "0.5.1"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [org.postgresql/postgresql "9.2-1003-jdbc4"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/tools.logging "0.2.6"]]
  :main nepse-data.web
  :min-lein-version "2.0.0")