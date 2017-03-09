(ns joplin.dynamodb.database
  (:require [clj-time
             [coerce :as tc]
             [core :as t]]
            [joplin.core :refer :all]
            [ragtime.protocols :refer [DataStore]]
            [taoensso.faraday :as far]))

(defn- ensure-migration-schema
  [client migration-table]
  (far/ensure-table client migration-table
                    [:id :s]
                    {:throughput {:read 1 :write 1}
                     :block?     true}))

;; ============================================================================
;; Ragtime interface

(defrecord DynamoDatabase [access-key secret-key endpoint migration-table]
  DataStore
  (add-migration-id [db id]
    (ensure-migration-schema db migration-table)
    (far/put-item db migration-table
                  {:id         id
                   :created-at (tc/to-long (t/now))}))
  (remove-migration-id [db id]
    (ensure-migration-schema db migration-table)
    (far/delete-item db migration-table {:id id}))
  (applied-migration-ids [db]
    (ensure-migration-schema db migration-table)
    (->> (far/scan db migration-table)
         (sort-by :created-at)
         (map :id))))

(defn- ->DynamoDatabase [target]
  (map->DynamoDatabase (-> (:db target)
                           (select-keys [:access-key :secret-key :endpoint :migration-table])
                           (update :migration-table #(or % :migrations)))))

;; ============================================================================
;; Joplin interface

(defmethod migrate-db :dynamo [target & args]
  (apply do-migrate (get-migrations (:migrator target))
         (->DynamoDatabase target) args))

(defmethod rollback-db :dynamo [target amount-or-id & args]
  (apply do-rollback (get-migrations (:migrator target))
         (->DynamoDatabase target) amount-or-id args))

(defmethod seed-db :dynamo [target & args]
  (apply do-seed-fn (get-migrations (:migrator target))
         (->DynamoDatabase target) target args))

(defmethod pending-migrations :dynamo [target & args]
  (do-pending-migrations (->DynamoDatabase target)
                         (get-migrations (:migrator target))))

(defmethod create-migration :dynamo [target id & args]
  (do-create-migration target id "joplin.dynamodb.database"))
