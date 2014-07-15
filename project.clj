(defproject joplin "0.1.0"
  :description "Flexible datastore migration and seeding"
  :url "http://github.com/martintrojer/joplin"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [joplin.core "0.1.0"]
                 [joplin.jdbc "0.1.0"]
                 [joplin.elasticsearch "0.1.0"]
                 [joplin.zookeeper "0.1.0"]
                 [ragtime "0.3.7"]]
  :plugins [[lein-sub "0.3.0"]]
  :sub ["joplin.core" "joplin.jdbc" "joplin.elasticsearch" "joplin.zookeeper"])
