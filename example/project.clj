(defproject joplin-example "0.3.0"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [joplin.core "0.3.0"]
                 [joplin.cassandra "0.3.0"]]

  :resource-paths ["joplin"]

  :profiles {:h2   {:dependencies [[com.h2database/h2 "1.3.171"]]}
             :psql {:dependencies [[postgresql/postgresql "9.3-1101.jdbc4"]]}})
