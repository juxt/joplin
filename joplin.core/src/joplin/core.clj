(ns joplin.core
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.java.io :as io]
            [clojure.set]
            [clojure.string]
            [ragtime.core :refer [applied-migration-ids]]
            [ragtime.main]))

;; ==========================================================================
;; methods implemented by migrator/seeder targets

(defmulti migrate-db
  "Migrate target database described by a joplin database map."
  (fn [target & args] (get-in target [:db :type])))

(defmulti rollback-db
  "Rollback target database described by a joplin database map N steps
(N defaults to 1, and is optionally present in the args)."
  (fn [target & args] (get-in target [:db :type])))

(defmulti seed-db
  "Migrate target database described by a joplin database map."
  (fn [target & args] (get-in target [:db :type])))

(defmulti reset-db
  "Reset (re-migrate and seed) target database described by a joplin database map."
  (fn [target options & args] (get-in target [:db :type])))

(defmulti create-migration
  "Create migrations file(s) for target database described by a joplin database map."
  (fn [target options & args] (get-in target [:db :type])))

;; ==========================================================================
;; Helpers

(def verbose-migration @#'ragtime.main/verbose-migration)

(defn load-var
  "Load a var specified by a string"
  [v]
  (try
    (@#'ragtime.main/load-var v)
    (catch Exception e
      (println (format "Function '%s' not found" v))
      (println (.getMessage e)))))

(defn get-full-migrator-id [id]
  (str (f/unparse (f/formatter "YYYYMMddHHmmss") (t/now)) "-" id))

(defn- get-migration-ns [path]
  (let [ns (->> (clojure.string/split path #"/")
                rest
                (interpose ".")
                (apply str))
        folder (io/file path)]
    (->> (.listFiles folder)
         (map #(.getName %))
         (map #(re-matches #"(.*)(\.clj)$" %))
         (keep second)
         (map #(clojure.string/replace % "_" "-"))
         sort
         (mapv #(vector % (symbol (str ns "." %)))))))

(defn get-migrations
  "Get all seq of ragtime migrators given a path (will scan the filesystem)"
  [path]
  (for [[id ns] (get-migration-ns path)]
    (do
      (require ns)
      (verbose-migration
       {:id id
        :up (load-var (str ns "/up"))
        :down (load-var (str ns "/down"))}))))

(defn do-rollback
  "Perform a rollback on a database"
  [migrations db n-str]
  (println "**" migrations db n-str (applied-migration-ids db))
  (doseq [m migrations]
    (ragtime.core/remember-migration m))
  (ragtime.core/rollback-last db (or (when n-str (Integer/parseInt n-str))
                                     1)))

(defn do-seed-fn
  "Run a seeder function with migration check"
  [migrations applied-migrations target args]
  (when-let [seed-fn (load-var (:seed target))]
    (let [migrations (set migrations)
          applied-migrations (set applied-migrations)]

      (when (not= (count migrations) (count applied-migrations))
        (println "There are" (- (count migrations) (count applied-migrations)) "pending migration(s)")
        (println (clojure.set/difference migrations applied-migrations))
        (System/exit 1))

      (when-not seed-fn
        (System/exit 1))

      (println "Appying seed function" (:seed target))
      (apply seed-fn target args))))

(defn do-reset
  "Perform a reset on a database"
  [db target args]

  ;; Roll back all
  (while (not-empty (ragtime.core/applied-migration-ids db))
    (apply rollback-db target args))

  ;; Migrate
  (apply migrate-db target args)

  ;; Seed
  (apply seed-db target args))
