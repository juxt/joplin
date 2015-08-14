(ns migrate
  (:require [clojure.java.io :as io]
            [joplin.repl :as repl]
            [ragtime.strategy :as strategy]))

;; check out resauce

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

  )

;; Zookeeper
(comment
  (def conf (repl/load-config
             (io/resource "joplin-zk.edn")))

  ;; seed zk
  (repl/seed conf :dev)
  )

;; SQL
(comment

  ;; override migration table name for sql


  )
