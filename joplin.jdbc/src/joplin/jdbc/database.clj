(ns joplin.jdbc.database
  (:use [joplin.core])
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [ragtime.core :as ragtime]
            [ragtime.sql.database :refer [map->SqlDatabase]]
            [ragtime.sql.files]))

(def run-sql-fn @#'ragtime.sql.files/run-sql-fn)
(def migration-pattern @#'ragtime.sql.files/migration-pattern)

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

(defn- get-sql-migrations [paths]
  (let [paths (if (sequential? paths) paths [paths])
        migrations (flatten (map get-sql-migrations' paths))]
    (when (some empty? migrations)
      (println "Warning, no migrators found!"))
    migrations))

(defn- get-db [target]
  (ragtime/connection (get-in target [:db :url])))

;; ============================================================================
;; SQL driven sql migrations, ragtime style

(defmethod migrate-db :sql [target & args]
  (do-migrate (get-sql-migrations (:migrator target)) (get-db target)))

(defmethod rollback-db :sql [target & [n]]
  (do-rollback (get-sql-migrations (:migrator target))
               (get-db target)
               n))

(defmethod seed-db :sql [target & args]
  (let [migrations (get-sql-migrations (:migrator target))]
    (do-seed-fn migrations (get-db target) target args)))

(defmethod reset-db :sql [target & args]
  (do-reset (get-sql-migrations (:migrator target))
            (get-db target) target args))

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
  (do-migrate (get-migrations (:migrator target))
              (map->SqlDatabase (append-uri target))))

(defmethod rollback-db :jdbc [target & [n]]
  (do-rollback (get-migrations (:migrator target))
               (map->SqlDatabase (append-uri target)) n))

(defmethod seed-db :jdbc [target & args]
  (let [migrations (get-migrations (:migrator target))]
    (do-seed-fn migrations (map->SqlDatabase (append-uri target))
                target args)))

(defmethod reset-db :jdbc [target & args]
  (do-reset (get-migrations (:migrator target))
            (map->SqlDatabase (append-uri target)) target args))

(defmethod create-migration :jdbc [target & [id]]
  (do-create-migration target id "joplin.jdbc.database"))
