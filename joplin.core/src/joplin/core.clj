(ns joplin.core
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.java.classpath :as classpath]
            [clojure.java.io :as io]
            [clojure.set]
            [clojure.string :as string]
            [ragtime.core]
            [ragtime.main]))


;; ==========================================================================
;; methods implemented by migrator/seed targets

(defmulti migrate-db
  "Migrate target database described by a joplin database map."
  (fn [target & args] (get-in target [:db :type])))

(defmulti rollback-db
  "Rollback target database described by a joplin database map N steps.
N is an optional argument and if present should be the first in args."
  (fn [target & args] (get-in target [:db :type])))

(defmulti seed-db
  "Migrate target database described by a joplin database map."
  (fn [target & args] (get-in target [:db :type])))

(defmulti reset-db
  "Reset (re-migrate and seed) target database described by a joplin database map."
  (fn [target & args] (get-in target [:db :type])))

(defmulti create-migration
  "Create migrations file(s) for target database described by a joplin database map.
The first argument must be the name of the migration to create"
  (fn [target & args] (get-in target [:db :type])))

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

(defn get-files [path]
  (let [folder (io/file path)
        classpath-folder (->> (string/split path #"/")
                              rest (interpose "/") (apply str))]
    (if (.isDirectory folder)
      ;; If it's a folder just read the file from there
      (->> (.listFiles folder)
           (map #(vector % (.getName %))))

      ;; Try finding this path in a jar on the classpath
      (->> (classpath/classpath-jarfiles)
           (mapcat classpath/filenames-in-jar)
           (filter #(.startsWith % classpath-folder))
           (map #(vector (io/resource %) (.getName (io/file %))))))))

(defn- get-migration-namespaces [path]
  (when path
    (let [ns (->> (string/split path #"/")
                  rest
                  (interpose ".")
                  (apply str))]
      (->> (get-files path)
           (map second)
           (map #(re-matches #"(.*)(\.clj)$" %))
           (keep second)
           (map #(string/replace % "_" "-"))
           sort
           (mapv #(vector % (symbol (str ns "." %))))))))

(defn get-migrations
  "Get all seq of ragtime migrators given a path (will scan the filesystem)"
  [path]
  (for [[id ns] (get-migration-namespaces path)]
    (do
      (require ns)
      (verbose-migration
       {:id id
        :up (load-var (str ns "/up"))
        :down (load-var (str ns "/down"))}))))

(defn do-migrate
  "Perform migration on a database"
  [migrations db]
  (println "Migrating" db)
  (ragtime.core/migrate-all db migrations))

(defn do-rollback
  "Perform rollback on a database"
  [migrations db n-str]
  (println "Rolling back" db)
  (doseq [m migrations]
    (ragtime.core/remember-migration m))
  (ragtime.core/rollback-last db (or (when n-str (Integer/parseInt n-str))
                                     1)))

(defn do-seed-fn
  "Run a seeder function with migration check"
  [migrations db target args]
  (println "Seeding" db)
  (when-let [seed-fn (and (:seed target) (load-var (:seed target)))]
    (let [migrations (->> migrations (map :id) set)
          applied-migrations (set (ragtime.core/applied-migration-ids db))]

      (cond
       (not= (count migrations) (count applied-migrations))
       (do
         (println "There are" (- (count migrations) (count applied-migrations)) "pending migration(s)")
         (println (clojure.set/difference migrations applied-migrations)))

       seed-fn
       (do
         (println "Appying seed function" (:seed target))
         (apply seed-fn target args))

       :else
       (println "Skipping" (:seed target))))))

(defn do-reset
  "Perform a reset on a database"
  [db target args]

  ;; Roll back all
  (ragtime.core/rollback-last db Integer/MAX_VALUE)

  ;; Migrate
  (apply migrate-db target args)

  ;; Seed
  (apply seed-db target args))

(defn do-create-migration
  "Create a scaffold migrator file"
  [target id ns]
  (when (:migrator target)
    (let [migration-id (get-full-migrator-id id)
          path (str (:migrator target) "/"
                    (string/replace migration-id "-" "_")
                    ".clj")]
      (println "creating" path)
      (try
        (spit path (format "(ns %s
  (:use [%s]))

(defn up [db]
  ;; TODO - up migration code here
  )

(defn down [db]
  ;; TODO - down migration goes here
  )
" (apply str (interpose "."
                        (concat
                         (-> (:migrator target) (string/split #"/") rest)
                         [migration-id]))) ns))
        (catch Exception e
          (println "Error creating file" path))))))
