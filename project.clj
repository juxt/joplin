(defproject joplin "0.1.0-SNAPSHOT"
  :description "Flexible datastore migration and seeding"
  :url "http://github.com/martintrojer/joplin"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [joplin.core "0.1.0-SNAPSHOT"]
                 [joplin.jdbc "0.1.0-SNAPSHOT"]
                 [joplin.elasticsearch "0.1.0-SNAPSHOT"]
                 [joplin.zookeeper "0.1.0-SNAPSHOT"]
                 [ragtime "0.3.7"]]
  :plugins [[lein-sub "0.3.0"]]
  :sub ["joplin.core" "joplin.jdbc" "joplin.elasticsearch" "joplin.zookeeper"])
