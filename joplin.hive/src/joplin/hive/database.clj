(ns joplin.hive.database
  (:require [clojure.java.jdbc :as jdbc]
            [joplin.core :as joplin]
            [ragtime.core :refer [Migratable]]))

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
  Migratable
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

(defmethod joplin/migrate-db :hive [target & args]
  (joplin/do-migrate
   (joplin/get-migrations (:migrator target))
   (->HiveDatabase target)))

(defmethod joplin/rollback-db :hive [target & [n]]
  (joplin/do-rollback
   (joplin/get-migrations (:migrator target))
   (->HiveDatabase target)
   n))

(defmethod joplin/seed-db :hive [target & args]
  (let [migrations (joplin/get-migrations (:migrator target))]
    (joplin/do-seed-fn migrations (->HiveDatabase target) target args)))

(defmethod joplin/reset-db :hive [target & args]
  (joplin/do-reset
   (joplin/get-migrations (:migrator target))
   (->HiveDatabase target) target args))

(defmethod joplin/create-migration :hive [target & [id]]
  (joplin/do-create-migration target id "joplin.hive.database"))

(defmethod pending-migrations :hive [target & args]
  (do-pending-migrations (->HiveDatabase target)
                         (get-migrations (:migrator target))))
