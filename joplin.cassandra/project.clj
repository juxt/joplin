(defproject joplin.cassandra "0.3.9-SNAPSHOT"
  :description "Cassandra support for Joplin"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [joplin.core "0.3.9-SNAPSHOT"]
                 [cc.qbits/alia "3.1.10"]
                 [cc.qbits/hayt "3.2.0"]])
