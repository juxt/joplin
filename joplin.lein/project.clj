(defproject joplin.lein "0.1.0-SNAPSHOT"
  :description "Flexible datastore migrations and seeds"
  :url "http://github.com/martintrojer/joplin"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[leinjacker "0.4.1"]

                 ;; remove
                 [com.h2database/h2 "1.3.160"]]
  :eval-in-leiningen true

  :source-paths ["src" "joplin"]

  ;; test
  :joplin {
           :migrators {
                       :mig "joplin/migrators/psql"
                       }
           :seeds {
                   :seed     "seeds.psql/run"
                   }
           :databases {
                       :psql-dev {:type :jdbc, :url "jdbc:h2:file:test_db";;"jdbc:h2:mem:test_db"
                                  }
                       :es-dev   {:type :es, :host "foo", :port 9200}
                       :zk-dev   {:type :zk, :host "foo", :port 2181}
                       }
           :environments {
                          :dev     [{:db :psql-dev, :migrator :mig, :seed :seed}
                                    ;;{:db :es-dev, :migrator :pz-es-dev}
                                    ;;{:db :zk-dev, :seed :pz-settings-dev}
                                    ]
                          }
           }
  )
