(ns joplin.zookeeper.database
  (:require [curator.framework :refer [curator-framework]]
            [joplin.core :refer :all]
            [joplin.zookeeper.exhibitor :refer [exhibitor-framework]]
            [zookeeper :as zk]))

(def ^:private charset "UTF-8")

(defn- ser [data]
  (.getBytes (pr-str data) ^String charset))

(defn- deser [data]
  (when data
    (read-string (String. data ^String charset))))

(defn- connection-string [target]
  (str (-> target :db :host) ":" (-> target :db :port)))

;; ==============================================================================
;; Raw / Curator / Exhibitor connection helpers

(defn- raw? [client]
  (instance? org.apache.zookeeper.ZooKeeper client))

(defn- connect-raw [target]
  (zk/connect (connection-string target) :timeout-msec 10000))

(defn- connect-curator [target]
  (when-let [client (curator-framework (connection-string target))]
    (.start client)
    client))

(defn- connect-exhibitor [target]
  (when-let [client (exhibitor-framework (-> target :db :host)
                                         (-> target :db :port))]
    (.start client)
    client))

(defn- close-raw [client]
  (zk/close client))

(defn- close-curator [client]
  (.close client))

(defn connect [target]
  (condp = (-> target :db :client)
    :curator (connect-curator target)
    :exhibitor (connect-exhibitor target)
    (connect-raw target)))

(defn close [client]
  (if (raw? client)
    (close-raw client)
    (close-curator client)))

;; ==============================================================================
;; ZK-client helpers

(defn- write-data-raw [client path data]
  (when-not (zk/exists client path)
    (zk/create-all client path :persistent? true))
  (let [v (:version (zk/exists client path))]
    (zk/set-data client path (ser data) v)))

(defn- write-data-curator [client path data]
  (when-not (.. client checkExists (forPath path))
    (.. client create (forPath path)))
  (.. client setData (forPath path (ser data))))

(defn- read-data-raw [client path]
  (deser (:data (zk/data client path))))

(defn- read-data-curator [client path]
  (deser (.. client getData (forPath path))))

(defn- delete-data-raw [client path]
  (zk/delete-all client path))

(defn write-data [client path data]
  (if (raw? client)
    (write-data-raw client path data)
    (write-data-curator client path data)))

(defn read-data [client path]
  (if (raw? client)
    (read-data-raw client path)
    (read-data-curator client path)))

(defn delete-data [client path]
  (if (raw? client)
    (delete-data-raw client path)
    (println "joplin.zookeeper: no delete implementation for curator connections")))

;; ==============================================================================

(defmethod seed-db :zk [target & args]
  (println "Seeding #joplin.zookeeper.database.ZK" (select-keys (:db target) [:host :port :client]))
  (when-let [seed-fn (and (:seed target) (load-var (:seed target)))]
    (println "Applying seed function" seed-fn)
    (apply @seed-fn target args)))

;; Dummy fns for migrations, doesn't really make sense for a k/v stores

(defmethod migrate-db :zk [target & args])
(defmethod rollback-db :zk [target & args])
(defmethod pending-migrations :zk [target & args])
(defmethod create-migration :zk [target & args])
