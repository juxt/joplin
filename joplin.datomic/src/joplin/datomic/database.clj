(ns joplin.datomic.database
  (:use [joplin.core])
  (:require [datomic.api :as d]
            [ragtime.core :refer [Migratable]]))

(defn transact-schema [conn schema]
  @(d/transact conn schema))

;; =============================================================

(defn- has-attribute?
  "Does database have an attribute named attr-name?"
  [db attr-name]
  (-> (d/entity db attr-name)
      :db.install/_attribute
      boolean))

(defn- ensure-migration-schema
  "Ensures the migration schema is loaded"
  [conn]
  (when-not (has-attribute? (d/db conn) :migrations/version)
    (transact-schema conn
                      [{:db/ident :migrations/id
                        :db/valueType :db.type/string
                        :db/cardinality :db.cardinality/one
                        :db/id #db/id [:db.part/db]
                        :db.install/_attribute :db.part/db}
                       {:db/ident :migrations/created-at
                        :db/valueType :db.type/instant
                        :db/cardinality :db.cardinality/one
                        :db/id #db/id [:db.part/db]
                        :db.install/_attribute :db.part/db}
                       ])))

(defn- get-connection [url]
  (d/create-database url)
  (d/connect url))

;; ============================================================================
;; Ragtime interface

(defrecord DatomicDatabase [url]
  Migratable
  (add-migration-id [db id]
    (when-let [conn (get-connection (:url db))]
      (ensure-migration-schema conn)
      @(d/transact conn [{:db/id #db/id [:db.part/user -100]
                          :migrations/id id
                          :migrations/created-at (java.util.Date.)}])))
  (remove-migration-id [db id]
    (when-let [conn (get-connection (:url db))]
      (ensure-migration-schema conn)
      (when-let [mig (first (d/q '[:find ?e
                                   :in $ ?id
                                   :where
                                   [?e :migrations/id ?id]]
                                 (d/db conn) id))]
        @(d/transact conn [[:db.fn/retractEntity mig]]))))
  (applied-migration-ids [db]
    (when-let [conn (get-connection (:url db))]
      (ensure-migration-schema conn)
      (->> (d/q '[:find ?id ?created-at
                  :where
                  [?e :migrations/id ?id]
                  [?e :migrations/created-at ?created-at]]
                (d/db conn))
           (sort-by second)
           (map first)))))

(defn- ->DTDatabase [target]
  (->DatomicDatabase (-> target :db :url)))

;; ============================================================================
;; Joplin interface

(defmethod migrate-db :dt [target & args]
  (do-migrate (get-migrations (:migrator target)) (->DTDatabase target)))

(defmethod rollback-db :dt [target & [n]]
  (do-rollback (get-migrations (:migrator target))
               (->DTDatabase target)
               n))

(defmethod seed-db :dt [target & args]
  (let [migrations (get-migrations (:migrator target))]
    (do-seed-fn migrations (->DTDatabase target) target args)))

(defmethod reset-db :dt [target & args]
  (do-reset (->DTDatabase target) target args))

(defmethod create-migration :dt [target & [_ _ id]]
  (do-create-migration target id "joplin.datomic.database"))
