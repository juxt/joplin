(ns joplin.jdbc.database
  (:use [joplin.core])
  (:require [clojure.java.io :as io]
            [ragtime.core :as ragtime]
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

(defn- get-sql-migrations [path]
     (->> path
          get-files
          (filter migration?)
          (sort-by second)
          (group-by migration-id)
          (map make-migration)
          (sort-by :id)
          (map verbose-migration)))

(defn- get-db [target]
  (ragtime/connection (get-in target [:db :url])))

(defmethod migrate-db :jdbc [target & args]
  (do-migrate (get-sql-migrations (:migrator target)) (get-db target)))

(defmethod rollback-db :jdbc [target & [n]]
  (do-rollback (get-sql-migrations (:migrator target))
               (get-db target)
               n))

(defmethod seed-db :jdbc [target & args]
  (let [migrations (get-sql-migrations (:migrator target))]
    (do-seed-fn migrations (get-db target) target args)))

(defmethod reset-db :jdbc [target & args]
  (do-reset (get-db target) target args))

(defmethod create-migration :jdbc [target & [id]]
  (let [migration-id (get-full-migrator-id id)
        path-up (str (:migrator target) "/" migration-id ".up.sql")
        path-down (str (:migrator target) "/" migration-id ".down.sql")]
    (println "creating" path-up)
    (spit path-up "SELECT 1")
    (println "creating" path-down)
    (spit path-down "SELECT 2")))
