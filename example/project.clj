(defproject joplin-example "0.1.3"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.h2database/h2 "1.3.160"]]
  :plugins [[joplin.lein "0.1.3"]]

  :source-paths ["src" "joplin"]

  :joplin {
           :migrators {
                       :pmig "joplin/migrators/psql"
                       :pes "joplin/migrators/es"
                       }
           :seeds {
                   :pseed "seeds.psql/run"
                   :zseed "seeds.zk/run"
                   :eseed "seeds.es/run"
                   }
           :databases {
                       :psql-dev {:type :jdbc,:url "jdbc:h2:file:test_db"}
                       :es-dev   {:type :es, :host "localhost", :port 9300, :cluster "dev"}
                       :zk-dev   {:type :zk, :host "localhost", :port 9090, :client :exhibitor}
                       }
           :environments {
                          :dev [
                                {:db :psql-dev, :migrator :pmig, :seed :pseed}
                                {:db :es-dev, :migrator :pes}
                                {:db :zk-dev, :seed :zseed}
                                ]
                          }
           })
