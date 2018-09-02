(ns joplin.cassandra.database
  (:require [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [joplin.core :refer :all]
            [ragtime.protocols :refer [DataStore]])
  (:import com.datastax.driver.core.exceptions.AlreadyExistsException))

;; =============================================================

(defn ensure-migration-schema
  "Ensures the migration schema is loaded"
  [conn]
  (alia/execute conn
                (hayt/create-table :migrations
                                   (hayt/if-not-exists)
                                   (hayt/column-definitions {:id          :varchar
                                                             :created_at  :timestamp
                                                             :primary-key [:id]}))))

(defn- cluster-configuration
  [{:keys [hosts credentials] :as cass-db}]
  {:contact-points hosts
   :credentials credentials})

(defn get-connection [db]
  (alia/connect (alia/cluster (cluster-configuration db)) (:keyspace db)))

(defn with-connection [db f]
  (when-let [conn (get-connection db)]
    (try
      (f conn)
      (finally (alia/shutdown conn)))))

;; ============================================================================
;; Ragtime interface

(defrecord CassandraDatabase [hosts keyspace]
  DataStore
  (add-migration-id [db id]
    (with-connection db
      (fn [conn]
        (ensure-migration-schema conn)
        (alia/execute conn
                      (hayt/insert :migrations
                                   (hayt/values {:id id, :created_at (java.util.Date.)}))))))
  (remove-migration-id [db id]
    (with-connection db
      (fn [conn]
        (ensure-migration-schema conn)
        (alia/execute conn
                      (hayt/delete :migrations
                                   (hayt/where {:id id}))))))

  (applied-migration-ids [db]
    (with-connection db
      (fn [conn]
        (ensure-migration-schema conn)
        (->> (alia/execute conn
                           (hayt/select :migrations))
             (sort-by :created_at)
             (map :id))))))

(defn- ->CassDatabase [target]
  (map->CassandraDatabase (:db target)))

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
