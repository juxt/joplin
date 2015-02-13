;; boot2docker tends to give me this IP
(def TARGET-HOST "192.168.59.103")

;; check ES
;; http://192.168.59.103:9200/users/_search?pretty=true&q=*:*

(defproject joplin-example "0.2.8"
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :plugins [[joplin.lein "0.2.8"]]

  :source-paths ["src" "joplin"]

  :joplin {:migrators {:es-mig           "joplin/migrators/es"
                       :es-data-mig      "joplin/migrators/esdata"
                       :sql-mig          "joplin/migrators/sql"
                       :sql-mig-extra    "joplin/migrators/sql/extrapath"
                       :imported-sql-mig "resources/imported-migrators/sql"
                       :jdbc-mig         "joplin/migrators/jdbc"
                       :jdbc-mig-extra   "joplin/migrators/jdbc/extrapath"
                       :hive-mig         "joplin/migrators/hive"
                       :cass-mig         "joplin/migrators/cass"
                       :dt-mig           "joplin/migrators/datomic"}
           :seeds     {:es-seed           "seeds.es/run"
                       :sql-seed          "seeds.sql/run"
                       :imported-sql-seed "imported-seeds.sql/run"
                       :cass-seed         "seeds.cass/run"
                       :dt-seed           "seeds.dt/run"
                       :zk-seed           "seeds.zk/run"}}

  :profiles {:dev
             {:joplin
              {:databases    {:dt-dev        {:type :dt, :url ~(str "datomic:free://" TARGET-HOST ":4334/test")}
                              :sql-dev       {:type :sql, :url "jdbc:h2:file:dev"}
                              :sql-dev-extra {:type :sql, :migration-table "ragtime_migrations_extra", :url "jdbc:h2:file:dev"}}
               :environments {:dev [{:db :dt-dev, :migrator :dt-mig, :seed :dt-seed}
                                    {:db :sql-dev, :migrator :imported-sql-mig, :seed :imported-sql-seed}
                                    {:db :sql-dev-extra, :migrator :sql-mig-extra}]}}}

             :web-frontend
             {:dependencies [[postgresql/postgresql "9.3-1101.jdbc4"]]
              :joplin
              {:databases    {:es-dev      {:type :es, :host ~TARGET-HOST, :port 9200}
                              :es-dev-data {:type :es, :host ~TARGET-HOST, :port 9200 :migration-index "migrations-data"}
                              :es-prod     {:type :es, :host "es-prod", :port 9200}
                              :sql-dev     {:type :sql, :url ~(str "jdbc:postgresql://" TARGET-HOST "/test?user=postgres&password=password")}
                              :sql-prod    {:type :sql, :url "jdbc:postgresql://psq-prod/prod?user=prod&password=secret"}}
               :environments {:dev  [{:db :sql-dev, :migrator :sql-mig, :seed :sql-seed}
                                     {:db :es-dev, :migrator :es-mig, :seed :es-seed}
                                     ;; the same as :es-dev, to enable multiple migration tables
                                     {:db :es-dev-data, :migrator :es-data-mig}]
                              :prod [{:db :sql-prod, :migrator :imported-sql-mig, :seed :imported-sql-seed}
                                     {:db :es-prod, :migrator :es-mig, :seeds :es-seed}]}}}

             :analysis
             {:dependencies [[com.h2database/h2 "1.3.171"]]
              :joplin
              {:databases    {:cass-dev       {:type :cass, :hosts [~TARGET-HOST], :keyspace "test"}
                              :jdbc-dev       {:type :jdbc, :url "jdbc:h2:file:analysis"}
                              :jdbc-dev-extra {:type :jdbc, :migration-table "ragtime_migrations_extra", :url "jdbc:h2:file:analysis"}
                              :zk-dev         {:type :zk, :host ~TARGET-HOST, :port 2181, :client :curator}
                              :zk-prod        {:type :zk, :host "zk-prod", :port 2181, :client :exhibitor}}
               :environments {:dev  [{:db :cass-dev, :migrator :cass-mig, :seed :cass-seed}
                                     {:db :jdbc-dev, :migrator :jdbc-mig, :seed :sql-seed}
                                     {:db :jdbc-dev-extra, :migrator :jdbc-mig-extra, :seed :sql-seed}
                                     {:db :zk-dev, :seed :zk-seed}]
                              :prod [{:db :zk-prod}]}}}

             :hive
             {:joplin
              {:databases    {:hive-dev {:type :hive, :subname ~(str "//" TARGET-HOST ":10000/default")}}
               :environments {:dev [{:db :hive-dev, :migrator :hive-mig}]}}}})
