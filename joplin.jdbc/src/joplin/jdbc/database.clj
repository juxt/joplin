(ns joplin.jdbc.tmp)

(ns joplin.jdbc.database
  (:use [joplin.core])
  (:require [clojure.java.io :as io]
            [ragtime.core :as ragtime :refer [Migratable connection]]
            [ragtime.sql.files])
  (:import java.io.FileNotFoundException
           java.util.Date
           java.text.SimpleDateFormat))

(defn require-jdbc [ns-alias]
  (try
    (require 'clojure.java.jdbc.deprecated)
    (alias ns-alias 'clojure.java.jdbc.deprecated)
    (catch FileNotFoundException ex
      (require 'clojure.java.jdbc)
      (alias ns-alias 'clojure.java.jdbc))))

(require-jdbc 'sql)


(def run-sql-fn @#'ragtime.sql.files/run-sql-fn)
(def migration-pattern @#'ragtime.sql.files/migration-pattern)

(def ^:dynamic *migration-table* "ragtime_migrations")

(defn ^:internal ensure-migrations-table-exists [db]
  ;; TODO: is there a portable way to detect table existence?
  (sql/with-connection db
    (try
      (sql/create-table *migration-table*
                        [:id "varchar(255)"]
                        [:created_at "varchar(32)"])
      (catch Exception _))))

(defn format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/insert-values *migration-table*
                         [:id :created_at]
                         [(str id) (format-datetime (Date.))])))

  (remove-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/delete-rows *migration-table* ["id = ?" id])))

  (applied-migration-ids [db]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/with-query-results results
        [(str "SELECT id FROM " *migration-table* " ORDER BY created_at")]
        (vec (map :id results))))))


(defn- migration? [[_ filename]]
  (re-find migration-pattern filename))

(defn- migration-id [[file filename]]
  (second (re-find migration-pattern filename)))

(defn- make-migration [[id [[down _] [up _]]]]
  {:id   id
   :up   (run-sql-fn up)
   :down (run-sql-fn down)})

(defn- get-sql-migrations' [path]
  (->> path
       get-files
       (filter migration?)
       (sort-by second)
       (group-by migration-id)
       (map make-migration)
       (sort-by :id)
       (map verbose-migration)))

(defn- get-sql-migrations [path]
  (let [migrations (get-sql-migrations' path)]
    (when (empty? migrations)
      (println "Warning, no migrators found!"))
    migrations))

;; ============================================================================
;; SQL driven sql migrations, ragtime style

(defn- get-table [target]
  (or (get-in target [:db :migration-table]) "ragtime_migrations"))

(defn- get-db [target]
  (map->SqlDatabase {:connection-uri (get-in target [:db :url])}))

(defmethod migrate-db :sql [target & args]
  (binding [*migration-table* (get-table target)]
    (do-migrate (get-sql-migrations (:migrator target)) (get-db target))))

(defmethod rollback-db :sql [target & [n]]
  (binding [*migration-table* (get-table target)]
    (do-rollback (get-sql-migrations (:migrator target))
                 (get-db target)
                 n)))

(defmethod seed-db :sql [target & args]
  (binding [*migration-table* (get-table target)]
    (let [migrations (get-sql-migrations (:migrator target))]
      (do-seed-fn migrations (get-db target) target args))))

(defmethod reset-db :sql [target & args]
  (binding [*migration-table* (get-table target)]
    (do-reset (get-sql-migrations (:migrator target))
              (get-db target) target args)))

(defmethod create-migration :sql [target & [id]]
  (let [migration-id (get-full-migrator-id id)
        path-up (str (:migrator target) "/" migration-id ".up.sql")
        path-down (str (:migrator target) "/" migration-id ".down.sql")]
    (println "creating" path-up)
    (spit path-up "SELECT 1")
    (println "creating" path-down)
    (spit path-down "SELECT 2")))

;; ============================================================================
;; Code driven sql migrations

(defn- append-uri [target]
  (assoc target :connection-uri (get-in target [:db :url])))

(defmethod migrate-db :jdbc [target & args]
  (binding [*migration-table* (get-table target)]
    (do-migrate (get-migrations (:migrator target))
                (map->SqlDatabase (append-uri target)))))

(defmethod rollback-db :jdbc [target & [n]]
  (binding [*migration-table* (get-table target)]
    (do-rollback (get-migrations (:migrator target))
                 (map->SqlDatabase (append-uri target)) n)))

(defmethod seed-db :jdbc [target & args]
  (binding [*migration-table* (get-table target)]
    (let [migrations (get-migrations (:migrator target))]
      (do-seed-fn migrations (map->SqlDatabase (append-uri target))
                  target args))))

(defmethod reset-db :jdbc [target & args]
  (binding [*migration-table* (get-table target)]
    (do-reset (get-migrations (:migrator target))
              (map->SqlDatabase (append-uri target)) target args)))

(defmethod create-migration :jdbc [target & [id]]
  (do-create-migration target id "joplin.jdbc.database"))
