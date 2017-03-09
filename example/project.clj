(defproject joplin-example "0.3.11-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [joplin.core "0.3.11-SNAPSHOT"]
                 [joplin.jdbc "0.3.11-SNAPSHOT"]
                 [com.h2database/h2 "1.4.188"]
                 [org.postgresql/postgresql "9.4-1201-jdbc4"]
                 [joplin.cassandra "0.3.11-SNAPSHOT"]
                 [joplin.dynamodb "0.3.11-SNAPSHOT"]
                 [joplin.zookeeper "0.3.11-SNAPSHOT"]
                 [joplin.elasticsearch "0.3.11-SNAPSHOT"]
                 [joplin.datomic "0.3.11-SNAPSHOT" :exclusions [joda-time]]
                 [joplin.hive "0.3.11-SNAPSHOT"]
                 ]

  :aliases {"migrate" ["run" "-m" "joplin.alias/migrate" "joplin.edn"]
            "seed" ["run" "-m" "joplin.alias/seed" "joplin.edn"]
            "rollback" ["run" "-m" "joplin.alias/rollback" "joplin.edn"]
            "reset" ["run" "-m" "joplin.alias/reset" "joplin.edn"]
            "pending" ["run" "-m" "joplin.alias/pending" "joplin.edn"]
            "create" ["run" "-m" "joplin.alias/create" "joplin.edn"]}

  :resource-paths ["joplin"]
  :repl-options {:init-ns migrate
                 :init (say-hello)})
