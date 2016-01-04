(defproject joplin-example "0.3.6-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [joplin.core "0.3.6-SNAPSHOT"]
                 [joplin.jdbc "0.3.6-SNAPSHOT"]
                 [com.h2database/h2 "1.4.188"]
                 [org.postgresql/postgresql "9.4-1201-jdbc4"]
                 [joplin.cassandra "0.3.6-SNAPSHOT"]
                 [joplin.dynamodb "0.3.6-SNAPSHOT"]
                 [joplin.zookeeper "0.3.6-SNAPSHOT"]
                 [joplin.elasticsearch "0.3.6-SNAPSHOT"]
                 [joplin.datomic "0.3.6-SNAPSHOT" :exclusions [joda-time]]
                 [joplin.hive "0.3.6-SNAPSHOT"]
                 ]

  :aliases {"migrate" ["run" "-m" "alias/migrate"]
            "seed" ["run" "-m" "alias/seed"]
            "rollback" ["run" "-m" "alias/rollback"]
            "reset" ["run" "-m" "alias/reset"]
            "pending" ["run" "-m" "alias/pending"]
            "create" ["run" "-m" "alias/create"]}

  :resource-paths ["joplin"]
  :repl-options {:init-ns migrate
                 :init (say-hello)})
