(ns joplin.jdbc.database
  (:require [ragtime.jdbc :refer [map->SqlDatabase]]
            [ragtime.protocols :refer [DataStore]]
            [joplin.core :refer :all]))

;; ============================================================================
;; SQL driven sql migrations, ragtime style

;; (defmethod migrate-db :sql [target & args]
;;   (binding [*migration-table* (get-table target)]
;;     (do-migrate (get-sql-migrations (:migrator target)) (get-db target))))

;; (defmethod rollback-db :sql [target & [n]]
;;   (binding [*migration-table* (get-table target)]
;;     (do-rollback (get-sql-migrations (:migrator target))
;;                  (get-db target)
;;                  n)))

;; (defmethod seed-db :sql [target & args]
;;   (binding [*migration-table* (get-table target)]
;;     (let [migrations (get-sql-migrations (:migrator target))]
;;       (do-seed-fn migrations (get-db target) target args))))

;; (defmethod reset-db :sql [target & args]
;;   (binding [*migration-table* (get-table target)]
;;     (do-reset (get-sql-migrations (:migrator target))
;;               (get-db target) target args)))

;; (defmethod create-migration :sql [target & [id]]
;;   (let [migration-id (get-full-migrator-id id)
;;         path-up (str (:migrator target) "/" migration-id ".up.sql")
;;         path-down (str (:migrator target) "/" migration-id ".down.sql")]
;;     (println "creating" path-up)
;;     (spit path-up "SELECT 1")
;;     (println "creating" path-down)
;;     (spit path-down "SELECT 2")))

;; (defmethod pending-migrations :sql [target & args]
;;   (binding [*migration-table* (get-table target)]
;;     (do-pending-migrations (get-db target)
;;                            (get-sql-migrations (:migrator target)))))

;; ============================================================================
;; Code driven sql migrations


(defn- setup-dbspec [m]
  (assoc m :db-spec {:connection-uri (:url m)}))

(defn- append-uri [target]
  (-> (merge {:migrations-table "ragtime_migrations"}
             (:db target))
      (select-keys [:url :datasource :migrations-table])
      setup-dbspec
      (merge target)))

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
