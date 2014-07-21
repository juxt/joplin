(defproject joplin.datomic "0.1.6-SNAPSHOT"
  :description "Datomic support for Joplin"
  :url "http://github.com/martintrojer/joplin"
  :scm {:name "git"
        :url "https://github.com/martintrojer/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-free "0.9.4815.12"]
                 [joplin.core "0.1.6-SNAPSHOT"]])
