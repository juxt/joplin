(ns joplin.elasticsearch.database
  (:use [joplin.core])
  (:require [clojure.string]
            [clojurewerkz.elastisch.native :as es]
            [clojurewerkz.elastisch.native.document :as esd]
            [clojurewerkz.elastisch.native.index :as esi]
            [ragtime.core :refer [Migratable]])
  (:import [org.elasticsearch.action.admin.indices.settings.get GetSettingsRequest]
           [org.elasticsearch.client Client]))

;; ============================================================================
;; ES connection

(def ^:dynamic *es-client* nil)

(defn- ensure-connected []
  (if *es-client* *es-client*
      (throw (Exception. "es-client has not been initialised"))))

(defn- wait-for-ready [shards]
  (-> (ensure-connected)
      .admin
      .cluster
      (.prepareHealth (into-array String []))
      ;; Don't wait for green -- it may never come for the cluster.
      .setWaitForYellowStatus
      (.setWaitForActiveShards shards)
      .execute
      .actionGet))

(defn init [{:keys [cluster host port]}]
  (println "Connecting to ES server" cluster host port)
  (when-not *es-client*
    (println "Building an es client")
    (let [client (es/connect [[host port]] {"cluster.name" cluster})]
      (alter-var-root #'*es-client* (fn [_] client))))
  (println "es client is" *es-client*)
  (println "readiness of es cluster:" (wait-for-ready 0)))

(defn init-local [client]
  (alter-var-root #'*es-client* (fn [_] client)))

;; ============================================================================
;; Handle migration tracking

(def migration-index "migrations")
(def migration-type "migration")
(def migration-document-id "0")

(defn- ensure-migration-index []
  (let [es-client (ensure-connected)]
    (if-not (esi/exists? es-client migration-index)
      (do
        (wait-for-ready 0)
        (esi/create es-client migration-index)
        (wait-for-ready 1)))))

(defn- es-get-applied []
  (let [es-client (ensure-connected)]
    (ensure-migration-index)
    (->> (esd/get es-client migration-index migration-type migration-document-id)
         :_source
         :migrations)))

(defn es-get-applied-migrations []
  (->> (es-get-applied)
       (sort-by second)
       keys
       (map name)))

(defn es-add-migration-id [migration-id]
  (let [es-client (ensure-connected)]
    (esd/put es-client migration-index migration-type migration-document-id
             {:migrations (assoc (es-get-applied) migration-id (java.util.Date.))})))

(defn es-remove-migration-id [migration-id]
  (let [es-client (ensure-connected)]
    (esd/put es-client migration-index migration-type migration-document-id
             {:migrations (dissoc (es-get-applied) (keyword migration-id))})))

;; ============================================================================
;; Data migration

(defn migrate-data
  ([old-index mapping-type new-index]
     (migrate-data old-index mapping-type new-index identity))
  ([old-index mapping-type new-index trans-f]
     (let [es-client (ensure-connected)]
       (dorun
        (->> (esd/search old-index
                         mapping-type
                         :query {:match_all {}})
             (esd/scroll-seq)
             (map :_source)
             (map trans-f)
             (pmap (fn [doc]
                     (esd/create new-index mapping-type doc :id (:_id doc)))))))))

;; ============================================================================
;; Functions for use within migrations

(defn- unwrap-settings [settings]
  (reduce (fn [m s] (assoc m (.key s) (-> s .value .getAsStructuredMap))) {} settings))

(defn get-settings
  "Not implemented for native client in elastisch yet so rolling our own"
  [client]
  (let [req (GetSettingsRequest.)]
    (-> ^Client client .admin .indices (.getSettings req) .actionGet
        .getIndexToSettings unwrap-settings)))

(defn find-index-names [alias-name]
  (->> (get-settings (ensure-connected))
       keys
       (map name)
       (filter #(.startsWith % alias-name))
       sort
       reverse))

(defn assign-alias
  "Add an index to an alias, optionally taking an old index name to be remove from the
   alias"
  [alias-name new-index-name & [old-index-name]]
  (let [ops [{:add {:indices new-index-name :alias alias-name}}]
        ops (if old-index-name
              (conj ops {:remove {:index old-index-name
                                  :aliases alias-name}})
              ops)]
    (println "ops" ops)
    (esi/update-aliases (ensure-connected) ops)))

(defn create-index
  "Create an index with the specified options.  Only the alias name is specified, the
   actual index name is auto-generated to avoid conflicts."
  [alias-name & opts]
  (let [new-index-name (str alias-name "-" (System/currentTimeMillis))
        old-index-name (first (find-index-names alias-name))]
    (apply esi/create (ensure-connected) new-index-name opts)
    (assign-alias alias-name new-index-name old-index-name)
    (wait-for-ready 1)))

(defn- deep-merge [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn update-index
  "Create an index based on a previous index, with updates applied."
  [alias-name & updates]
  (let [es-client (ensure-connected)
        current-index (first (find-index-names alias-name))
        mappings (:mappings ((esi/get-mapping es-client current-index) (keyword current-index)))
        settings (-> ((get-settings es-client) (name current-index))
                     (update-in ["index"] dissoc "uuid" "version"))
        update-map (apply hash-map updates)
        mappings-u (or (:mappings update-map) {})
        settings-u (or (:settings update-map) {})]
    (create-index alias-name
                  :mappings (deep-merge mappings mappings-u)
                  :settings (deep-merge settings settings-u))))

(defn- rollback-index-to [alias-name current previous]
  (assign-alias alias-name previous current)
  (esi/delete (ensure-connected) current))

(defn- drop-index [alias-name & indexes]
  (let [es-client (ensure-connected)]
    (esi/update-aliases es-client (map #(hash-map :remove {:index % :aliases alias-name}) indexes))
    (doseq [index indexes]
      (esi/delete es-client index))))

(defn rollback-index
  "Rolls back an index (i.e. points the alias at the previous version of the index, then
   drops the most recent index).  If no previous index exists the current index and the
   alias are both deleted"
  [alias-name]
  (let [index-names (find-index-names alias-name)
        current (first index-names)
        previous (second index-names)]
    (if previous
      (rollback-index-to alias-name current previous)
      (drop-index alias-name current))))

;; ============================================================================
;; Ragtime interface

(defrecord ElasticSearchDatabase [host port cluster]
  Migratable
  (add-migration-id [db id]
    (es-add-migration-id id))
  (remove-migration-id [db id]
    (es-remove-migration-id id))
  (applied-migration-ids [db]
    (es-get-applied-migrations)))

(defn ->ESDatabase [target]
  (map->ElasticSearchDatabase (select-keys (:db target) [:host :port :cluster])))

;; ============================================================================
;; Joplin interface

(defmethod migrate-db :es [target & args]
  (init (:db target))
  (do-migrate (get-migrations (:migrator target)) (->ESDatabase target)))

(defmethod rollback-db :es [target & [n]]
  (init (:db target))
  (do-rollback (get-migrations (:migrator target))
               (->ESDatabase target)
               n))

(defmethod seed-db :es [target & args]
  (init (:db target))
  (let [migrations (get-migrations (:migrator target))]
    (do-seed-fn migrations (->ESDatabase target) target args)))

(defmethod reset-db :es [target & args]
  (init (:db target))
  (do-reset (->ESDatabase target) target args))

(defmethod create-migration :es [target & [id]]
  (do-create-migration target id "joplin.elasticsearch.database"))
