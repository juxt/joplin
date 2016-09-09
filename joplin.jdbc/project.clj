(defproject joplin.jdbc "0.3.8"
  :description "JDBC support for Joplin"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [joplin.core "0.3.8"]
                 [ragtime/ragtime.jdbc "0.6.3"]])
