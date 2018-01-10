(ns joplin.core
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [clojure.java
             [classpath :as classpath]
             [io :as io]]
            [clojure.set :as set]
            [clojure.string :as string]
            [ragtime
             [core]
             [protocols]
             [repl]
             [strategy]])
  (:import
   [java.io File]
   [java.util.jar JarFile]))

;; ==========================================================================
;; methods implemented by migrator/seed targets

(defmulti migrate-db
  "Migrate target database described by a joplin database map."
  (fn [target & args] (get-in target [:db :type])))

(defmulti rollback-db
  "Rollback target database described by a joplin database map N steps.
N is an optional argument and if present should be the first in args."
  (fn [target amount-or-id & args] (get-in target [:db :type])))

(defmulti seed-db
  "Migrate target database described by a joplin database map."
  (fn [target & args] (get-in target [:db :type])))

(defmulti create-migration
  "Create migrations file(s) for target database described by a joplin database map.
The first argument must be the name of the migration to create"
  (fn [target id & args] (get-in target [:db :type])))

(defmulti pending-migrations
  "Return a list of pending migrations."
  (fn [target & args] (get-in target [:db :type])))

;; ==========================================================================
;; Helpers

(defn get-full-migrator-id
  "Get a string with current date and time prepended"
  [id]
  (str (f/unparse (f/formatter "YYYYMMddHHmmss") (t/now)) "-" id))

(defn load-var
  "Load a var specified by a string"
  [var-name]
  (try
    (let [var-sym (symbol var-name)]
      (require (-> var-sym namespace symbol))
      (if-let [res (find-var var-sym)]
        res
        (throw (Exception.))))
    (catch Exception e
      (printf "Function '%s' not found\n" var-name))))

(defn get-fn [var]
  "Resolves a var"
  (if (and var (bound? var))
    (deref var)
    (printf "Var '%s' couldn't be de-reffed, probably a compiler error\n" var)))

(defn drop-first-part [path delimiter]
  (->> (string/split path #"/")
       rest
       (interpose delimiter)
       (apply str)))

(defn- classpath-directories []
  (filter #(and
            (instance? File %)
            (.isDirectory ^File %))
          (classpath/system-classpath)))

(defn- classpath-jarfiles []
  (->> (classpath/system-classpath)
       (filter classpath/jar-file?)
       (map #(java.util.jar.JarFile. ^java.io.File %))))

(defn- get-files
  "Get migrations files given a folder path.
Will try to locate files on the local filesystem, folder on the classpath
or resource folders inside a jar on the classpath"
  [path]
  (let [local-folder          (io/file path)
        classpath-folder-name (drop-first-part path "/")
        folder-on-classpath   (->> (classpath-directories)
                                   (map #(str (.getPath ^File %) "/" classpath-folder-name))
                                   (map io/file)
                                   (filter #(.isDirectory ^File %))
                                   first)]

    (cond
     ;; If it's a local folder just read the file from there
     (.isDirectory local-folder)
     (->> (.listFiles local-folder)
          (map #(vector % (.getName ^File %))))

     ;; If it's a folder on the classpath use that
     folder-on-classpath
     (->> (.listFiles ^File folder-on-classpath)
          (map #(vector % (.getName ^File %))))

     ;; Try finding this path inside a jar on the classpath
     :else
     (->> (classpath-jarfiles)
          (mapcat classpath/filenames-in-jar)
          (filter #(.startsWith ^String % classpath-folder-name))
          (map #(vector (io/resource %) (.getName (io/file %))))))))

(defn- get-migration-namespaces
  "Get a sequence on namespaces containing the migrations on a given folder path"
  [path]
  (when path
    (let [ns (drop-first-part path ".")]
      (->> (get-files path)
           (map second)
           (map #(re-matches #"(.*)(\.clj)$" %))
           (keep second)
           (map #(string/replace % "_" "-"))
           sort
           (mapv #(vector % (symbol (str ns "." %))))))))

(defrecord JoplinMigration [id up down]
  ragtime.protocols/Migration
  (id [_] id)
  (run-up! [_ db] (up db))
  (run-down! [_ db] (down db)))

(defn get-migrations
  "Get all seq of ragtime migrators given a path
(will scan the filesystem and classpath)"
  [path]
  (let [migration-namespaces (get-migration-namespaces path)]
    (when (empty? migration-namespaces)
      (println "No migrators found"))
    (for [[id ns] migration-namespaces]
      (do
        (require ns)
        (map->JoplinMigration {:id   id
                               :up   (get-fn (load-var (str ns "/up")))
                               :down (get-fn (load-var (str ns "/down")))})))))


(def split-at-conflict @#'ragtime.strategy/split-at-conflict)

(defn- get-pending-migrations [db migrations]
  (let [migrations            (map :id migrations)
        applied-migrations    (ragtime.protocols/applied-migration-ids db)
        not-applied           (set/difference (set migrations) (set applied-migrations))
        [conflicts unapplied] (split-at-conflict applied-migrations migrations)]
    (when (seq conflicts)
      (throw (Exception. (str "Conflict! Expected " (first unapplied)
                              " but " (first conflicts) " was applied."))))
    (sort not-applied)))

(defn do-migrate
  "Perform migration on a database"
  [migrations db & [opts]]
  (println "Migrating" db)
  (ragtime.repl/migrate (merge {:datastore  db
                                :migrations migrations}
                               opts)))

(defn do-rollback
  "Perform rollback on a database"
  [migrations db amount-or-id & [opts]]
  (println "Rolling back" db)
  (ragtime.repl/rollback (merge {:datastore  db
                                 :migrations migrations}
                                opts)
                         amount-or-id))

(defn do-seed-fn
  "Run a seeder function with migration check"
  [migrations db target & args]
  (println "Seeding" db)
  (when-let [seed-fn (and (:seed target) (get-fn (load-var (:seed target))))]
    (let [skip-migration-check? (:skip-migration-check? (first args))
          pending-migrations (if skip-migration-check?
                               []
                               (get-pending-migrations db migrations))]
      (cond
        (not-empty pending-migrations)
        (do
          (printf "There are %d pending migration(s)\n" (count pending-migrations))
          (println pending-migrations))

        seed-fn
        (do
          (printf "Applying seed function %s\n" seed-fn)
          (apply seed-fn target args))

        :else
        (printf "Skipping %s\n" (:seed target))))))

(defn do-pending-migrations [db migrations]
  (println "Pending migrations" (get-pending-migrations db migrations)))

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
  (:require [%s :refer :all]))

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
