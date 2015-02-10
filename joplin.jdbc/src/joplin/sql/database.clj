(ns joplin.sql.database
  (:require [ragtime.core :refer [Migratable]])
  (:import java.io.FileNotFoundException java.util.Date java.text.SimpleDateFormat))

;; This ns contains code copied over from ragtime.sql.database while we wait for
;; https://github.com/weavejester/ragtime/pull/61 to be merged

(defn- require-jdbc [ns-alias]
  (try
    (require 'clojure.java.jdbc.deprecated)
    (alias ns-alias 'clojure.java.jdbc.deprecated)
    (catch FileNotFoundException ex
      (require 'clojure.java.jdbc)
      (alias ns-alias 'clojure.java.jdbc))))

(require-jdbc 'sql)

(def ^:dynamic *migration-table* "ragtime_migrations")

(defn- ensure-migrations-table-exists [db]
  ;; TODO: is there a portable way to detect table existence?
  (sql/with-connection db
    (try
      (sql/create-table *migration-table*
                        [:id "varchar(255)"]
                        [:created_at "varchar(32)"])
      (catch Exception _))))


(defn- format-datetime [dt]
  (-> (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSS")
      (.format dt)))

(defrecord SqlDatabase []
  Migratable
  (add-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/insert-values *migration-table*
                         [:id :created_at]
                         [(str id) (format-datetime (Date.))])))

  (remove-migration-id [db id]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/delete-rows *migration-table* ["id = ?" id])))

  (applied-migration-ids [db]
    (sql/with-connection db
      (ensure-migrations-table-exists db)
      (sql/with-query-results results
        [(format "SELECT id FROM %s ORDER BY created_at" *migration-table*)]
        (vec (map :id results))))))

(defn get-db [target]
  (map->SqlDatabase {:connection-uri (get-in target [:db :url])}))
