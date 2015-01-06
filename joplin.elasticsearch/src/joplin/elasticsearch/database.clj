(ns joplin.elasticsearch.database
  (:use [joplin.core])
  (:require [clojure.string]
            [clj-time.format :as f]
            [clj-time.core :as t]
            [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.admin :as admin]
            [clojurewerkz.elastisch.rest.bulk :as bulk]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojure.walk :refer [stringify-keys]]
            [ragtime.core :refer [Migratable]])
  (:import [org.elasticsearch.action.admin.indices.settings.get GetSettingsRequest]
           [org.elasticsearch.client Client]))

;; ============================================================================
;; Handle migration tracking

(def migration-index "migrations")
(def migration-type "migration")
(def migration-document-id "0")

(defn- ensure-migration-index [es-client]
  (when-not (esi/exists? es-client migration-index)
    (esi/create es-client migration-index)))

(defn- es-get-applied [es-client]
  (ensure-migration-index es-client)
  (->> (esd/get es-client migration-index migration-type migration-document-id)
       :_source
       :migrations
       (stringify-keys)))

(defn es-get-applied-migrations [es-client]
  (->> (es-get-applied es-client)
       (sort-by second)
       keys
       (map name)))

(defn- timestamp-as-string []
  (f/unparse (f/formatters :date-time) (t/now)))

(defn es-add-migration-id [es-client migration-id]
  (esd/put es-client migration-index migration-type migration-document-id
           {:migrations (assoc (es-get-applied es-client) migration-id
                               (timestamp-as-string))}))

(defn es-remove-migration-id [es-client migration-id]
  (esd/put es-client migration-index migration-type migration-document-id
           {:migrations (dissoc (es-get-applied es-client) migration-id)}))

;; ============================================================================
;; Data migration

;; Copied from clojurewerkz.elastisch.rest.bulk - allows migrators to optionally migrate these fields too
;; excludes :_index
(def ^:private special-operation-keys
  [:_type :_id :_retry_on_conflict :_routing :_percolate :_parent :_timestamp :_ttl])

(defn migrate-data
  ([es-client old-index mapping-type new-index]
     (migrate-data es-client old-index mapping-type new-index identity))
  ([es-client old-index mapping-type new-index trans-f]
     (dorun
      (->> (esd/search es-client
                       old-index
                       mapping-type
                       :query {:match_all {}}
                       :scroll "1m")
           (esd/scroll-seq es-client)
           (partition-all 50)
           (pmap (fn [docs]
                   (let [updated-docs (map (fn [doc]
                                             (merge (select-keys doc special-operation-keys)
                                                    (trans-f (:_source doc))))
                                           docs)]
                     (bulk/bulk-with-index
                      es-client
                      new-index
                      (bulk/bulk-index updated-docs)))))))))

;; ============================================================================
;; Functions for use within migrations

(defn client [{:keys [host port]}]
  (es/connect (str "http://" host ":" port)))

(defn find-index-names [es-client alias-name]
  (->> (esi/get-settings es-client)
       keys
       (map name)
       (filter #(.startsWith % alias-name))
       sort
       reverse))

(defn assign-alias
  "Add an index to an alias, optionally taking an old index name to be remove from the
   alias"
  [es-client alias-name new-index-name & [old-index-name]]
  (let [ops [{:add {:index new-index-name :alias alias-name}}]
        ops (if old-index-name
              (conj ops {:remove {:index old-index-name
                                  :alias alias-name}})
              ops)]
    (apply esi/update-aliases es-client ops)))

(defn create-index
  "Create an index with the specified options.  Only the alias name is specified, the
   actual index name is auto-generated to avoid conflicts. Old and new index names returned
   so user can perform data migration."
  [es-client alias-name & opts]
  (let [new-index-name (str alias-name "-" (System/currentTimeMillis))
        old-index-name (first (find-index-names es-client alias-name))]
    (apply esi/create es-client new-index-name opts)
    (assign-alias es-client alias-name new-index-name old-index-name)
    [old-index-name new-index-name]))

(defn- deep-merge [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn- get-index-settings [es-client index]
  (-> (esi/get-settings es-client (name index))
      (get (keyword index))
      :settings))

(defn update-index
  "Update the mappings and settings of an existing index in place.

   You can change settings in a fairly straightforward manner.

   Mappings can also be changed, e.g. to add mappings for new fields.
   Care must be taken when updating mappings of existing fields - see the ES documentation - but you
   may well get unexpected behaviour. You can set :ignore-conflicts? to true if you know it is
   safe to do so.

   If you are making non-compatible changes to settings or mappings you should not use this function -
   you will need to use clone-index with your mapping updates
   and then migrate-data to copy the data into it."
  [es-client alias-name & updates]
  (let [current-index (first (find-index-names es-client alias-name))
        mappings (:mappings (get (esi/get-mapping es-client current-index) (keyword current-index)))
        settings (-> (get-index-settings es-client current-index)
                     (update-in [:index] dissoc :uuid :version))
        update-map (apply hash-map updates)
        mappings-updates (or (:mappings update-map) {})
        settings-updates (or (:settings update-map) {})
        ignore-conflicts? (or (:ignore-conflicts? update-map) false)]

    (doseq [[type _] mappings-updates]
      (let [new-mappings (select-keys (deep-merge mappings mappings-updates) [type])
            result (esi/update-mapping es-client alias-name (name type) {:mapping new-mappings
                                                                         :ignore_conflicts ignore-conflicts?})]
        (when (:error result)
          (println result))))

    (when (not-empty settings-updates)
      (let [result (esi/update-settings es-client alias-name settings-updates)]
        (when (:error result)
          (println result))))))

(defn clone-index
  "Create a new index with settings and mappings copied from source index,
   with optional updates to mappings or settings.

   Use when you need to change a setting that affects the way existing data has been indexed.

   To move the data as well, use migrate-data."
  [es-client source-index-alias target-index-alias & updates]
  (let [source-index (first (find-index-names es-client source-index-alias))
        mappings (:mappings (get (esi/get-mapping es-client source-index) (keyword source-index)))
        settings (-> (get-index-settings es-client source-index)
                     (update-in [:index] dissoc :uuid :version))
        update-map (apply hash-map updates)
        mappings-updates (or (:mappings update-map) {})
        settings-updates (or (:settings update-map) {})]

    (create-index es-client
                  target-index-alias
                  :mappings (deep-merge mappings mappings-updates)
                  :settings (deep-merge settings settings-updates))))

(defn- rollback-index-to [es-client alias-name current previous]
  (assign-alias es-client alias-name previous current)
  (esi/delete es-client current))

(defn- drop-index [es-client alias-name & indexes]
  (apply esi/update-aliases es-client (map #(hash-map :remove {:index % :alias alias-name}) indexes))
  (doseq [index indexes]
    (esi/delete es-client index)))

(defn rollback-index
  "Rolls back an index (i.e. points the alias at the previous version of the index, then
   drops the most recent index).  If no previous index exists the current index and the
   alias are both deleted"
  [es-client alias-name]
  (let [index-names (find-index-names es-client alias-name)
        current (first index-names)
        previous (second index-names)]
    (if previous
      (rollback-index-to es-client alias-name current previous)
      (drop-index es-client alias-name current))))

;; ============================================================================
;; Ragtime interface

(defrecord ElasticSearchDatabase [host port index]
  Migratable
  (add-migration-id [db id]
    (es-add-migration-id (client db) id))
  (remove-migration-id [db id]
    (es-remove-migration-id (client db) id))
  (applied-migration-ids [db]
    (es-get-applied-migrations (client db))))

(defn ->ESDatabase [target]
  (map->ElasticSearchDatabase (select-keys (:db target) [:host :port])))

;; ============================================================================
;; Joplin interface

(defmethod migrate-db :es [target & args]
  (do-migrate (get-migrations (:migrator target)) (->ESDatabase target)))

(defmethod rollback-db :es [target & [n]]
  (do-rollback (get-migrations (:migrator target))
               (->ESDatabase target)
               n))

(defmethod seed-db :es [target & args]
  (let [migrations (get-migrations (:migrator target))]
    (do-seed-fn migrations (->ESDatabase target) target args)))

(defmethod reset-db :es [target & args]
  (do-reset (get-migrations (:migrator target))
            (->ESDatabase target) target args))

(defmethod create-migration :es [target & [id]]
  (do-create-migration target id "joplin.elasticsearch.database"))
