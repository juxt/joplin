(ns joplin.datomic.database
  (:require [datomic.api :as d]
            [joplin.core :refer :all]
            [ragtime.protocols :refer [DataStore]]))

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
  DataStore
  (add-migration-id [db id]
    (when-let [conn (get-connection (:url db))]
      (ensure-migration-schema conn)
      @(d/transact conn [{:db/id #db/id [:db.part/user -100]
                          :migrations/id id
                          :migrations/created-at (java.util.Date.)}])))
  (remove-migration-id [db id]
    (when-let [conn (get-connection (:url db))]
      (ensure-migration-schema conn)
      (when-let [mig (d/q '[:find ?e .
                            :in $ ?id
                            :where
                            [?e :migrations/id ?id]]
                          (d/db conn) id)]
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
  (apply do-migrate (get-migrations (:migrator target))
         (->DTDatabase target) args))

(defmethod rollback-db :dt [target amount-or-id & args]
  (apply do-rollback (get-migrations (:migrator target))
         (->DTDatabase target) amount-or-id args))

(defmethod seed-db :dt [target & args]
  (apply do-seed-fn (get-migrations (:migrator target))
         (->DTDatabase target) target args))

(defmethod pending-migrations :dt [target & args]
  (do-pending-migrations (->DTDatabase target)
                         (get-migrations (:migrator target))))

(defmethod create-migration :dt [target id & args]
  (do-create-migration target id "joplin.datomic.database"))
