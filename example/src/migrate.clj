(ns migrate
  (:require [joplin.alias :refer [*load-config*]]
            [clojure.java.io :as io]
            [joplin.repl :as repl
             :refer [migrate rollback seed reset create pending]]
            [ragtime.strategy :as strategy]))

(defn say-hello []
  (println "Welcome to Joplin!")
  (println "From this REPL you can try to migrate a lots of different datastores")
  (println " ")
  (def config (*load-config* "joplin.edn"))
  (println "The config is loaded into the var 'config'")
  (println "Please remember that you need to set the environment variable 'TARGET_HOST' for all the migrators to work properly")
  (println " ")
  (println "Example usage")
  (println "
  ;; Migrate (can also be called without the last database argument)
  (migrate config :dev :cass-dev)

  ;; override conflict strategy
  (migrate config :dev :cass-dev {:strategy strategy/rebase})

  ;; Rollback
  (rollback config :dev :cass-dev 1)

  ;; Seed (can also be called without the last database argument)
  (seed config :dev :cass-dev)

  ;; Reset
  (reset config :dev :cass-dev)

  ;; Create migration
  (create config :dev :cass-dev \"foo\")

  ;; See pending migrations
  (pending config :dev :cass-dev)

  ;; Load a config file, joplins repl/load-config has support for env variables
  (def conf (repl/load-config
             (io/resource \"joplin-cass.edn\")))

"))
