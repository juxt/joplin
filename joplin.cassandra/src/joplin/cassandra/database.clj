(ns joplin.cassandra.database
  (:use [joplin.core])
  (:require [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql    :as cql]
            [clojurewerkz.cassaforte.query  :as cq]
            [ragtime.core :refer [Migratable]])
  (:import [com.datastax.driver.core.exceptions AlreadyExistsException]))

;; =============================================================

(defn- ensure-migration-schema
  "Ensures the migration schema is loaded"
  [conn]
  (try
    (cql/create-table conn "migrations"
                      (cq/column-definitions {:id          :varchar
                                              :created_at  :timestamp
                                              :primary-key [:id]}))
    (catch AlreadyExistsException e)))

(defn get-connection [hosts keyspace]
  (when-let [conn (cc/connect hosts)]
    (cql/use-keyspace conn keyspace)
    conn))

;; ============================================================================
;; Ragtime interface

(defrecord CassandraDatabase [hosts keyspace]
  Migratable
  (add-migration-id [db id]
    (ensure-migration-schema (get-connection hosts keyspace))
    (cql/insert (get-connection hosts keyspace)
                "migrations"
                {:id id, :created_at (java.util.Date.)}))
  (remove-migration-id [db id]
    (ensure-migration-schema (get-connection hosts keyspace))
    (cql/delete (get-connection hosts keyspace)
                "migrations"
                (cq/where {:id id})))

  (applied-migration-ids [db]
    (ensure-migration-schema (get-connection hosts keyspace))
    (->> (cql/select (get-connection hosts keyspace)
                     "migrations")
         (sort-by :created_at)
         (map :id))))

(defn- ->CassDatabase [target]
  (map->CassandraDatabase (select-keys (:db target) [:hosts :keyspace])))

;; ============================================================================
;; Joplin interface

(defmethod migrate-db :cass [target & args]
  (do-migrate (get-migrations (:migrator target)) (->CassDatabase target)))

(defmethod rollback-db :cass [target & [n]]
  (do-rollback (get-migrations (:migrator target))
               (->CassDatabase target)
               n))

(defmethod seed-db :cass [target & args]
  (let [migrations (get-migrations (:migrator target))]
    (do-seed-fn migrations (->CassDatabase target) target args)))

(defmethod reset-db :cass [target & args]
  (do-reset (get-migrations (:migrator target))
            (->CassDatabase target) target args))

(defmethod create-migration :cass [target & [id]]
  (do-create-migration target id "joplin.cassandra.database"))

(defmethod pending-migrations :cass [target & args]
  (do-pending-migrations (->CassDatabase target)
                         (get-migrations (:migrator target))))
