(defproject joplin "0.1.7-SNAPSHOT"
  :description "Flexible datastore migration and seeding"
  :url "http://github.com/martintrojer/joplin"
  :scm {:name "git"
        :url "https://github.com/martintrojer/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [joplin.core "0.1.7-SNAPSHOT"]
                 [joplin.jdbc "0.1.7-SNAPSHOT"]
                 [joplin.elasticsearch "0.1.7-SNAPSHOT"]
                 [joplin.zookeeper "0.1.7-SNAPSHOT"]
                 [joplin.datomic "0.1.7-SNAPSHOT"]
                 [joplin.cassandra "0.1.7-SNAPSHOT"]
                 [ragtime "0.3.6"]]
  :plugins [[lein-sub "0.3.0"]]
  :sub ["joplin.core" "joplin.jdbc" "joplin.elasticsearch" "joplin.zookeeper"
        "joplin.datomic" "joplin.cassandra" "joplin.lein"])
