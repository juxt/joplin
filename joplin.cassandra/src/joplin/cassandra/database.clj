(ns joplin.cassandra.database
  (:require [qbits.alia :as alia]
            [qbits.hayt :as hayt]
            [joplin.core :refer :all]
            [ragtime.protocols :refer [DataStore]])
  (:import com.datastax.driver.core.SessionManager))

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
  [{:keys [hosts credentials port] :as cass-db
    :or {port 9042}}]
  {:contact-points hosts
   :port port
   :credentials credentials})

(defn get-connection [db]
  (alia/connect (alia/cluster (cluster-configuration db)) (:keyspace db)))

;; ============================================================================
;; Ragtime interface

(extend-protocol DataStore
  SessionManager
  (add-migration-id [this id]
    (ensure-migration-schema this)
    (alia/execute this
                  (hayt/insert :migrations
                               (hayt/values {:id id, :created_at (java.util.Date.)}))))
  (remove-migration-id [this id]
    (ensure-migration-schema this)
    (alia/execute this
                  (hayt/delete :migrations
                               (hayt/where {:id id}))))

  (applied-migration-ids [this]
    (ensure-migration-schema this)
    (->> (alia/execute this
                       (hayt/select :migrations))
         (sort-by :created_at)
         (map :id))))

;; ============================================================================
;; Joplin interface

(defmethod migrate-db :cass [target & args]
  (with-open [conn (get-connection (:db target))]
    (apply do-migrate (get-migrations (:migrator target))
           conn args)))

(defmethod rollback-db :cass [target amount-or-id & args]
  (with-open [conn (get-connection (:db target))]
    (apply do-rollback (get-migrations (:migrator target))
           conn amount-or-id args)))

(defmethod seed-db :cass [target & args]
  (with-open [conn (get-connection (:db target))]
    (apply do-seed-fn (get-migrations (:migrator target))
           conn target args)))

(defmethod pending-migrations :cass [target & args]
  (with-open [conn (get-connection (:db target))]
    (do-pending-migrations conn
                           (get-migrations (:migrator target)))))

(defmethod create-migration :cass [target id & args]
  (do-create-migration target id "joplin.cassandra.database"))
