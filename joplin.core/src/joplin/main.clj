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

(def help-text
  "Manage Joplin migrations and seeds.

Commands:
migrate env [db]    Migrate to the latest version
rollback env db [n] Rollback n versions (defaults to 1)
seed env [db]       Seed and environment with data
reset env [db]      Re-apply all migrations and/or seeds

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
      "migrate"  (migrate targets args)
      "rollback" (rollback targets args)
      "seed"     (seed targets args)
      "reset"    (reset targets args)
      "help"     (println help-text)
      (do (println help-text)
          (System/exit 1)))))


(comment

  (def test-options
    {
     :migrators {
                 :pz-psql "migrators/psql"
                 :pz-es   "migrators/es"
                 }
     :seeds {
             :pz-psql-dev     "seeds.psql-dev/run"
             :pz-psql-staging "seeds.psql-stg/run"
             :pz-es-dev       "seeds.es-dev/run"
             :pz-settings-dev "settings.dev/run"
             }
     :databases {
                 :psql-dev {:type :jdbc, :url "jdbc:h2:mem:test_db"}
                 :es-dev   {:type :es, :host "foo", :port 9200}
                 :zk-dev   {:type :zk, :host "foo", :port 2181}
                 }
     :environments {
                    :dev     [{:db :psql-dev, :migrator :pz-psql, :seed :pz-psql-dev}
                              {:db :es-dev, :migrator :pz-es-dev}
                              {:db :zk-dev, :seed :pz-settings-dev}]
                    :staging [{:db :psql-dev, :migrator :pz-sql, :seed :pz-psql-dev}]
                    }
     })

  (migrate
   (get-targets test-options :dev nil)
   {})

  )
