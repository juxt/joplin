(ns migrate
  (:require [clojure.java.io :as io]
            [joplin.repl :as repl]
            [ragtime.strategy :as strategy]))

(println "Welcome to ")

(defn- url [m]
  (assoc m :url (format (:urlf m) (:host m))))

;; SQL
(comment

  (def conf (-> (io/resource "joplin-jdbc.edn")
                repl/load-config
                (update-in [:databases :psql-dev] url)))

  (repl/migrate conf :dev :jdbc-dev)
  (repl/reset conf :dev :sql-dev-extra)


  )


;; Datomic
(comment
  (def conf (-> (io/resource "joplin-dt.edn")
                repl/load-config
                (update-in [:databases :dt-dev] url)
                (assoc-in [:databases :dt-dev :url] "datomic:mem://foo")))
  (repl/reset conf :dev)

  )

;; ES
(comment
  (def conf (repl/load-config
             (io/resource "joplin-es.edn")))

  (repl/migrate conf :dev)
  (repl/seed conf :dev)
  (repl/reset conf :dev :es-dev)

  )

;; Cassandra
(comment

  ;; Load the config file, joplins repl/load-config has support for env variables
  (def conf (repl/load-config
             (io/resource "joplin-cass.edn")))

  ;; Migrate cassandra
  (repl/migrate conf :dev)

  ;; override conflict strategy
  (repl/migrate conf :dev :cass-dev {:strategy strategy/rebase})

  ;; Rollback cassandra
  (repl/rollback conf :dev :cass-dev 1)

  ;; Seed cassandra
  (repl/seed conf :dev)

  ;; Reset cassandra
  (repl/reset conf :dev :cass-dev)

  ;; Create migration
  (repl/create conf :dev :cass-dev "foo")

  ;; See pending
  (repl/pending conf :dev :cass-dev)

  )

;; Zookeeper
(comment
  (def conf (repl/load-config
             (io/resource "joplin-zk.edn")))

  ;; seed zk
  (repl/seed conf :dev)
  )
