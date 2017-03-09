(defproject joplin "0.3.11-SNAPSHOT"
  :description "Flexible datastore migration and seeding"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :plugins [[lein-sub "0.3.0"]]
  :sub ["joplin.jdbc" "joplin.elasticsearch" "joplin.zookeeper"
        "joplin.datomic" "joplin.cassandra" "joplin.dynamodb" "joplin.hive"])
