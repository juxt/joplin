(defproject joplin.elasticsearch "0.1.5-SNAPSHOT"
  :description "ElasticSearch support for Joplin"
  :url "http://github.com/martintrojer/joplin"
  :scm {:name "git"
        :url "https://github.com/martintrojer/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clojurewerkz/elastisch "2.1.0-beta4"]
                 [joplin.core "0.1.5-SNAPSHOT"]])
