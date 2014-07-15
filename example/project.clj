(defproject joplin-example "0.1.4-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.h2database/h2 "1.3.160"]]
  :plugins [[joplin.lein "0.1.4-SNAPSHOT"]]

  :source-paths ["src" "joplin"]

  :joplin {
           :migrators {:sql-mig "joplin/migrators/sql"
                       :es-mig "joplin/migrators/es"}
           :seeds {:sql-seed "seeds.sql/run"
                   :es-seed "seeds.es/run"
                   :zk-seed "seeds.zk/run"}
           :databases {:sql-dev  {:type :jdbc, :url "jdbc:h2:mem:test"}
                       :sql-prod {:type :jdbc, :url "jdbc:h2:file:prod"}

                       :es-dev   {:type :es, :host "localhost", :port 9300, :cluster "dev"}
                       :es-prod  {:type :es, :host "es-prod", :port 9300, :cluster "dev"}

                       :zk-dev   {:type :zk, :host "localhost", :port 2181, :client :curator}
                       :zk-prod  {:type :zk, :host "zk-prod", :port 2181, :client :exhibitor}}

           :environments {:dev [{:db :sql-dev, :migrator :sql-mig, :seed :sql-seed}
                                {:db :es-dev, :migrator :es-mig, :seed :es-seed}
                                {:db :zk-dev, :seed :zk-seed}]
                          :prod [{:db :sql-prod, :migrator :sql-mig, :seed :sql-seed}
                                 {:db :es-prod, :migrator :es-mig}
                                 {:db :zk-prod}]}
           })
