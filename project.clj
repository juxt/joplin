(defproject joplin "0.1.12"
  :description "Flexible datastore migration and seeding"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [joplin.core "0.1.12"]
                 [joplin.jdbc "0.1.12"]
                 [joplin.elasticsearch "0.1.12"]
                 [joplin.zookeeper "0.1.12"]
                 [joplin.datomic "0.1.12"]
                 [joplin.cassandra "0.1.12"]
                 [ragtime "0.3.6"]]
  :plugins [[lein-sub "0.3.0"]]
  :sub ["joplin.core" "joplin.jdbc" "joplin.elasticsearch" "joplin.zookeeper"
        "joplin.datomic" "joplin.cassandra" "joplin.lein"])
