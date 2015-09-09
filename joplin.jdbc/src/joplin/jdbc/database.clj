(ns joplin.jdbc.database
  (:require [clojure.set :as set]
            [joplin.core :refer :all]
            [ragtime.jdbc :refer [map->SqlDatabase load-resources load-directory]]
            [ragtime.protocols :refer [DataStore]]))

(defn- append-uri [target]
  (let [uri {:connection-uri (:url (:db target))}]
    (-> (merge {:migrations-table "ragtime_migrations"}
               (:db target))
        (select-keys [:url :datasource :migrations-table])
        (assoc :db-spec uri)
        (merge uri)
        (merge target))))

(defn- get-sql-migrations [path]
  (distinct
   (or (seq (load-directory path))
       (seq (load-resources path))
       (seq (load-resources (drop-first-part path "/"))))))

;; ============================================================================
;; SQL driven sql migrations, ragtime style

(defmethod migrate-db :sql [target & args]
  (apply do-migrate (get-sql-migrations (:migrator target))
         (map->SqlDatabase (append-uri target)) args))

(defmethod rollback-db :sql [target amount-or-id & args]
  (apply do-rollback (get-sql-migrations (:migrator target))
         (map->SqlDatabase (append-uri target)) amount-or-id args))

(defmethod seed-db :sql [target & args]
  (apply do-seed-fn (get-sql-migrations (:migrator target))
         (map->SqlDatabase (append-uri target)) target args))

(defmethod pending-migrations :sql [target & args]
  (do-pending-migrations (map->SqlDatabase (append-uri target))
                         (get-sql-migrations (:migrator target))))

(defmethod create-migration :sql [target & [id]]
  (let [migration-id (get-full-migrator-id id)
        path-up      (str (:migrator target) "/" migration-id ".up.sql")
        path-down    (str (:migrator target) "/" migration-id ".down.sql")]
    (println "creating" path-up)
    (spit path-up "SELECT 1")
    (println "creating" path-down)
    (spit path-down "SELECT 2")))

;; ============================================================================
;; Code driven sql migrations

(defmethod migrate-db :jdbc [target & args]
  (apply do-migrate (get-migrations (:migrator target))
         (map->SqlDatabase (append-uri target)) args))

(defmethod rollback-db :jdbc [target amount-or-id & args]
  (apply do-rollback (get-migrations (:migrator target))
         (map->SqlDatabase (append-uri target)) amount-or-id args))

(defmethod seed-db :jdbc [target & args]
  (apply do-seed-fn (get-migrations (:migrator target))
         (map->SqlDatabase (append-uri target)) target args))

(defmethod pending-migrations :jdbc [target & args]
  (do-pending-migrations (map->SqlDatabase (append-uri target))
                         (get-migrations (:migrator target))))

(defmethod create-migration :jdbc [target id & args]
  (do-create-migration target id "joplin.jdbc.database"))
