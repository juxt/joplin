(ns joplin.main
  (:use [clojure.tools.cli :only (cli)]
        [joplin.core])
  (:require [clojure.string :as str]
            [ragtime.main :as ragtime]))

(defn- parse-args [args]
  (cli args
       ["-r" "--require"]
       ["-e" "--environments"]
       ["-d" "--databases"]
       ["-m" "--migrators"]
       ["-s" "--seeds"]))

(defn get-targets
  "Get a seq of dbs filtered by optional target-db. Looks up and inlines :migrator and :seed information (these will be nil is non-existent)"
  [conf target-env target-db]
  (let [env     (get-in conf [:environments target-env])
        pop-key (fn [key coll] #(assoc % key (get-in conf [coll (key %)])))]
    (some->> env
             (filter #(or (nil? target-db) (= (:db %) target-db)))
             (map (pop-key :db :databases))
             (map (pop-key :migrator :migrators))
             (map (pop-key :seed :seeds)))))

(defn- run-op [f targets args] (doseq [t targets] (apply f t args)))

(def help-text
  "Manage Joplin migrations and seeds.

Commands:
migrate env [db]    Migrate to the latest version
rollback env db [n] Rollback n versions (defaults to 1)
seed env [db]       Seed and environment with data
reset env [db]      Re-apply all migrations and/or seeds
create env db id    Create a new migration for a given migration id

Options:
-r  --require       Comma-separated list of namespaces to require")

(defn -main
  [& args]
  (let [[conf [command & args]] (parse-args args)
        environment             (keyword (first args))
        database                (keyword (second args))
        targets                 (get-targets conf environment database)]

    (when (empty? (get-in conf [:environments environment]))
      (println (format "Could not find environment '%s'" environment))
      (System/exit 1))
    (when (and (second args) (nil? (get-in conf [:databases database])))
      (println (format "Could not find database '%s'" database))
      (System/exit 1))
    (when (empty? targets)
      (println "Could not find any matching targets")
      (System/exit 1))

    ;; require namespaces
    (doseq [ns (seq (ragtime/parse-namespaces conf))]
      (require ns))

    ;; dispatch commands
    (condp = command
      "migrate"  (run-op migrate-db targets args)
      "rollback" (run-op rollback-db targets args)
      "seed"     (run-op seed-db targets args)
      "reset"    (run-op reset-db targets args)
      "create"   (apply create-migration (first targets) args)
      "help"     (println help-text)
      (do (println help-text)
          (System/exit 1)))

    ;; Some plugins (datomic, cassandra) hold onto threads that makes joplin hang at this point
    (System/exit 0)))
