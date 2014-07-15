(ns joplin.jdbc.database
  (:use [joplin.core])
  (:require [ragtime.core :as ragtime]
            [ragtime.sql.files :as files]))

(defn- get-sql-migrations [path]
  (map verbose-migration (files/migrations path)))

(defn- get-db [target]
  (ragtime/connection (get-in target [:db :url])))

(defmethod migrate-db :jdbc [target & args]
  (ragtime/migrate-all
   (get-db target)
   (get-sql-migrations (:migrator target))))

(defmethod rollback-db :jdbc [target & [_ n]]
  (do-rollback (get-sql-migrations (:migrator target))
               (get-db target)
               n))

(defmethod seed-db :jdbc [target & args]
  (let [migrations (map :id (get-sql-migrations (:migrator target)))
        applied (ragtime/applied-migration-ids (get-db target))]
    (do-seed-fn migrations applied target args)))

(defmethod reset-db :jdbc [target & args]
  (do-reset (get-db target) target args))

(defmethod create-migration :jdbc [target & args])
