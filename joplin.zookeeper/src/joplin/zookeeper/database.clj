(ns joplin.zookeeper.database
  (:use [joplin.core])
  (:require [zookeeper :as zk]))

(def ^:private charset "UTF-8")

(defn- ser [data]
  (.getBytes (pr-str data) ^String charset))

(defn- deser [data]
  (when data
    (read-string (String. data ^String charset))))

(defn connect [target]
  (zk/connect (str (-> target :db :host) ":" (-> target :db :port))
              :timeout-msec 10000))

(defn close [client]
  (zk/close client))

(defn write-data [client path data]
  (when-not (zk/exists client path)
    (zk/create-all client path :persistent? true))
  (let [v (:version (zk/exists client path))]
    (zk/set-data client path (ser data) v)))

(defn read-data [client path]
  (deser (:data (zk/data client path))))

(defn delete-data [client path]
  (zk/delete-all client path))

(defmethod seed-db :zk [target & args]
  (when-let [seed-fn (load-var (:seed target))]
    (apply seed-fn target args)))

(defmethod reset-db :zk [target & args]
  (apply seed-db target args))

;; ========================
;; Dummy fns for migrations, doesn't really make sense for a k/v stores

(defmethod migrate-db :zk [target & args])
(defmethod rollback-db :zk [target & args])
