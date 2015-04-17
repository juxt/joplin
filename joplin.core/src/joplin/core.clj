(ns joplin.core
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.java.classpath :as classpath]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as string]
            [ragtime.core]
            [ragtime.main]
            [ragtime.strategy]))

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

(defmulti pending-migrations
  "Return a list of pending migrations."
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

(defn get-full-migrator-id
  "Get a string with current date and time prepended"
  [id]
  (str (f/unparse (f/formatter "YYYYMMddHHmmss") (t/now)) "-" id))

(defn get-files
  "Get migrations files given a folder path.
Will try to locate files on the local filesystem, folder on the classpath
or resource folders inside a jar on the classpath"
  [path]
  (let [local-folder (io/file path)
        classpath-folder-name (->> (string/split path #"/")
                                   rest (interpose "/") (apply str))
        folder-on-classpath (->> (classpath/classpath-directories)
                                 (map #(str (.getPath %) "/" classpath-folder-name))
                                 (map io/file)
                                 (filter #(.isDirectory %))
                                 first)]

    (cond
     ;; If it's a local folder just read the file from there
     (.isDirectory local-folder)
     (->> (.listFiles local-folder)
          (map #(vector % (.getName %))))

     ;; If it's a folder on the classpath use that
     folder-on-classpath
     (->> (.listFiles folder-on-classpath)
          (map #(vector % (.getName %))))

     ;; Try finding this path inside a jar on the classpath
     :else
     (->> (classpath/classpath-jarfiles)
          (mapcat classpath/filenames-in-jar)
          (filter #(.startsWith % classpath-folder-name))
          (map #(vector (io/resource %) (.getName (io/file %))))))))

(defn- get-migration-namespaces
  "Get a sequence on namespaces containing the migrations on a given folder path"
  [path]
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
  "Get all seq of ragtime migrators given a path
(will scan the filesystem and classpath)"
  [path]
  (let [migration-namespaces (get-migration-namespaces path)]
    (when (empty? migration-namespaces)
      (println "Warning, no migrators found!"))
    (for [[id ns] migration-namespaces]
      (do
        (require ns)
        (verbose-migration
         {:id id
          :up (load-var (str ns "/up"))
          :down (load-var (str ns "/down"))})))))


(def split-at-conflict @#'ragtime.strategy/split-at-conflict)

(defn- get-pending-migrations [db migrations]
  (let [migrations            (map :id migrations)
        applied-migrations    (ragtime.core/applied-migration-ids db)
        not-applied           (set/difference (set migrations) (set applied-migrations))
        [conflicts unapplied] (split-at-conflict applied-migrations migrations)]
    (when (seq conflicts)
      (throw (Exception. (str "Conflict! Expected " (first unapplied)
                              " but " (first conflicts) " was applied."))))
    (sort not-applied)))

(defn do-migrate
  "Perform migration on a database"
  [migrations db]
  (println "Migrating" db)
  (ragtime.core/migrate-all db migrations))

(defn do-rollback
  "Perform rollback on a database"
  [migrations db n]
  (println "Rolling back" db)
  (doseq [m migrations]
    (ragtime.core/remember-migration m))
  (ragtime.core/rollback-last db (cond
                                  (string? n) (Integer/parseInt n)
                                  (number? n) n
                                  :else 1)))

(defn do-seed-fn
  "Run a seeder function with migration check"
  [migrations db target args]
  (println "Seeding" db)
  (when-let [seed-fn (and (:seed target) (load-var (:seed target)))]
    (let [pending-migrations (get-pending-migrations db migrations)]

      (cond
       (not-empty pending-migrations)
       (do
         (println "There are" (count pending-migrations) "pending migration(s)")
         (println pending-migrations))

       seed-fn
       (do
         (println "Applying seed function" (:seed target))
         (apply seed-fn target args))

       :else
       (println "Skipping" (:seed target))))))

(defn do-reset
  "Perform a reset on a database"
  [migrations db target args]

  (println "Resetting" db)
  (doseq [m migrations]
    (ragtime.core/remember-migration m))

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
          ns-name (string/replace migration-id "_" "-")
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
                         [ns-name]))) ns))
        (catch Exception e
          (println "Error creating file" path))))))

(defn do-pending-migrations [db migrations]
  (println "Pending migrations" (get-pending-migrations db migrations)))
