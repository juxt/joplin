(ns joplin.jdbc.database
  (:use [joplin.core])
  (:require [ragtime.core :as ragtime]
            [ragtime.sql.files :as files]))

(defn- get-sql-migrations [path]
  (map verbose-migration (files/migrations path)))

(defn- get-db [target]
  (ragtime/connection (get-in target [:db :url])))

(defmethod migrate-db :jdbc [target & args]
  (do-migrate (get-sql-migrations (:migrator target)) (get-db target)))

(defmethod rollback-db :jdbc [target & [_ _ n]]
  (do-rollback (get-sql-migrations (:migrator target))
               (get-db target)
               n))

(defmethod seed-db :jdbc [target & args]
  (let [migrations (get-sql-migrations (:migrator target))]
    (do-seed-fn migrations (get-db target) target args)))

(defmethod reset-db :jdbc [target & args]
  (do-reset (get-db target) target args))

(defmethod create-migration :jdbc [target & [_ _ id]]
  (let [migration-id (get-full-migrator-id id)
        path-up (str (:migrator target) "/" migration-id ".up.sql")
        path-down (str (:migrator target) "/" migration-id ".down.sql")]
    (println "creating" path-up)
    (spit path-up "SELECT 1")
    (println "creating" path-down)
    (spit path-down "SELECT 2")))
