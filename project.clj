(defproject cmp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lib-noir "0.5.0"]
                 [compojure "1.1.5" :exclusions [ring/ring-core]]
                 [com.cemerick/friend "0.1.5"]
                 [ring-server "0.2.7"]
                 [clabango "0.5"]
                 [korma "0.3.0-RC5"]
                 [clj-json "0.5.3"] 
                 [mysql/mysql-connector-java "5.1.6"]
                 [com.taoensso/timbre "2.7.1"]
                 [com.taoensso/tower "2.0.1"]
                 [com.postspectacular/rotor "0.1.0"]
                 [markdown-clj "0.9.19"]
                 [clj-pdf "1.11.1"]
                 [dk.ative/docjure "1.6.0"]
                 [org.clojure/data.csv "0.1.2"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [clojurewerkz/quartzite "1.1.0"]
                 [clj-time "0.6.0"]
                 [log4j "1.2.17"
                  :exclusions
                  [javax.mail/mail
                   javax.jms/jms
                   com.sun.jdmk/jmxtools
                   com.sun.jmx/jmxri]]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]]
  :plugins [[lein-ring "0.8.7"]
            [lein-midje "3.0.0"]]
  :ring {:handler cmp.handler/war-handler
         :init    cmp.handler/init
         :destroy cmp.handler/destroy}
  :profiles
  {:production {:ring {:open-browser? false
                       :stacktraces?  false
                       :auto-reload?  false}}
   :dev {:dependencies [[ring-mock "0.1.3"]
                        [ring/ring-devel "1.1.8"]
                        [midje "1.5.1"]]}}
  :java-source-paths ["src/cmp/java"]
  :min-lein-version "2.0.0")
