(ns joplin.cassandra.database
  (:require [clojurewerkz.cassaforte
             [client :as cc]
             [cql :as cql]
             [query :as cq]]
            [joplin.core :refer :all]
            [ragtime.protocols :refer [DataStore]])
  (:import com.datastax.driver.core.exceptions.AlreadyExistsException))

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
  (cc/connect hosts keyspace))

(defn with-connection [hosts keyspace f]
  (when-let [conn (cc/connect hosts keyspace)]
    (try
      (f conn)
      (finally (cc/disconnect! conn)))))

;; ============================================================================
;; Ragtime interface

(defrecord CassandraDatabase [hosts keyspace]
  DataStore
  (add-migration-id [db id]
    (with-connection hosts keyspace
      (fn [conn]
        (ensure-migration-schema conn)
        (cql/insert conn
                    "migrations"
                    {:id id, :created_at (java.util.Date.)}))))
  (remove-migration-id [db id]
    (with-connection hosts keyspace
      (fn [conn]
        (ensure-migration-schema conn)
        (cql/delete conn
                    "migrations"
                    (cq/where {:id id})))))

  (applied-migration-ids [db]
    (with-connection hosts keyspace
      (fn [conn]
        (ensure-migration-schema conn)
        (->> (cql/select conn
                         "migrations")
             (sort-by :created_at)
             (map :id))))))

(defn- ->CassDatabase [target]
  (map->CassandraDatabase (select-keys (:db target) [:hosts :keyspace])))

;; ============================================================================
;; Joplin interface

(defmethod migrate-db :cass [target & args]
  (apply do-migrate (get-migrations (:migrator target))
         (->CassDatabase target) args))

(defmethod rollback-db :cass [target amount-or-id & args]
  (apply do-rollback (get-migrations (:migrator target))
         (->CassDatabase target) amount-or-id args))

(defmethod seed-db :cass [target & args]
  (apply do-seed-fn (get-migrations (:migrator target))
         (->CassDatabase target) target args))

(defmethod pending-migrations :cass [target & args]
  (do-pending-migrations (->CassDatabase target)
                         (get-migrations (:migrator target))))

(defmethod create-migration :cass [target id & args]
  (do-create-migration target id "joplin.cassandra.database"))
