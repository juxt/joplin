(ns joplin.hive.database
  (:require [clojure.java.jdbc :as jdbc]
            [joplin.core :refer :all]
            [ragtime.protocols :refer [DataStore]]))

;; =============================================================

(defn get-spec [subname]
  {:classname "org.apache.hive.jdbc.HiveDriver"
   :subname subname
   :subprotocol "hive2"})

(defn ensure-migration-schema
  "Ensures the migration schema is loaded"
  [db-spec]
  (try
    (->> "create table schema_migrations (id STRING)"
         (jdbc/db-do-prepared db-spec false))
    (catch Exception e)))

(defn schema-versions [db-spec]
  (ensure-migration-schema db-spec)
  (some->> ["show tblproperties schema_migrations"]
           (jdbc/query db-spec)
           (filter #(= (:prpt_name %) "avro.schema.versions"))
           (first)
           :prpt_value
           (read-string)))

(defn add-migration [db-spec id]
  (ensure-migration-schema db-spec)
  (let [versions (pr-str (conj (vec (schema-versions db-spec)) id))]
    (->> (format "alter table schema_migrations set tblproperties ('avro.schema.versions'= '%s')" versions)
         (jdbc/db-do-prepared db-spec false))))

(defn remove-migration [db-spec id]
  (ensure-migration-schema db-spec)
  (let [versions (pr-str (remove (partial = id) (schema-versions db-spec)))]
    (->> (format "alter table schema_migrations set tblproperties ('avro.schema.versions'= '%s')" versions)
         (jdbc/db-do-prepared db-spec false))))

;; ============================================================================
;; Ragtime interface

(defrecord HiveDatabase [subname]
  DataStore
  (add-migration-id [db id]
    (let [spec (get-spec subname)]
      (add-migration spec id)))

  (remove-migration-id [db id]
    (let [spec (get-spec subname)]
      (remove-migration spec id)))

  (applied-migration-ids [db]
    (let [spec (get-spec subname)]
      (schema-versions spec))))

(defn ->HiveDatabase [target]
  (map->HiveDatabase {:subname (:subname (:db target))}))

;; ============================================================================
;; Joplin interface

(defmethod migrate-db :hive [target & args]
  (apply do-migrate (get-migrations (:migrator target))
         (->HiveDatabase target) args))

(defmethod rollback-db :hive [target amount-or-id & args]
  (apply do-rollback (get-migrations (:migrator target))
         (->HiveDatabase target) amount-or-id args))

(defmethod seed-db :hive [target & args]
  (apply do-seed-fn (get-migrations (:migrator target))
         (->HiveDatabase target) target args))

(defmethod pending-migrations :hive [target & args]
  (do-pending-migrations (->HiveDatabase target)
                         (get-migrations (:migrator target))))

(defmethod create-migration :hive [target id & args]
  (do-create-migration target id "joplin.hive.database"))
