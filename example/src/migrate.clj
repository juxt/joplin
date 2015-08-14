(ns migrate
  (:require [clojure.java.io :as io]
            [joplin.repl :as repl]))


(comment

  ;; Load the config file, joplins repl/load-config has support for env variables
  (def conf (repl/load-config
             (io/resource "joplin-cass.edn")))

  ;; Migrate cassandra
  (repl/migrate conf :dev)

  ;; Rollback cassandra
  (repl/rollback conf :dev :cass-dev 1)

  ;; Seed cassandra
  (repl/seed conf :dev)

  ;; Reset cassandra
  (repl/reset conf :dev)

  )
