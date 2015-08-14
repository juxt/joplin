(defproject joplin-example "0.3.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [joplin.core "0.3.0"]
                 [joplin.jdbc "0.3.0"]
                 [com.h2database/h2 "1.4.188"]
                 [org.postgresql/postgresql "9.4-1201-jdbc4"]
                 ;; [joplin.cassandra "0.3.0"]
                 ;; [joplin.zookeeper "0.3.0"]
                 ;; [joplin.elasticsearch "0.3.0"]
                 ;; [joplin.datomic "0.3.0"]
                 ;; [joplin.hive "0.3.0"]
                 ]

  :resource-paths ["joplin"]
  :repl-options {:init-ns migrate})
