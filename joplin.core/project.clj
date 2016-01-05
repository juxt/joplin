(defproject joplin.core "0.3.6"
  :description "Flexible datastore migration and seeding"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :pedantic? :abort
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.classpath "0.2.3"]
                 [clj-time "0.11.0"]
                 [ragtime/ragtime.core "0.5.2"]])
