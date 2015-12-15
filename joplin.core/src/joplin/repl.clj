(ns joplin.repl
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [joplin.core :refer [migrate-db rollback-db seed-db pending-migrations
                                 create-migration]])
  (:import [java.io PushbackReader]))

(def ^:const libs
  {:dt     'joplin.datomic.database
   :cass   'joplin.cassandra.database
   :dynamo 'joplin.dynamodb.database
   :jdbc   'joplin.jdbc.database
   :sql    'joplin.jdbc.database
   :hive   'joplin.hive.database
   :es     'joplin.elasticsearch.database
   :zk     'joplin.zookeeper.database})

(defn- require-joplin-ns
  "We need to require the namespaces to get the defmethod evaluated"
  [conf]
  (let [types (->> conf
                   :databases
                   vals
                   (map :type)
                   (remove nil?))]
    (doseq [t types
            :when (contains? libs t)]
      (require (get libs t)))))

(defn- get-targets
  "Get a seq of dbs filtered by optional target-db. Looks up and inlines :migrator and :seed information (these will be nil is non-existent)"
  [conf target-env target-db]
  (let [env     (get-in conf [:environments target-env])
        pop-key (fn [key coll] #(assoc % key (get-in conf [coll (key %)])))]
    (some->> env
             (filter #(or (nil? target-db) (= (:db %) target-db)))
             (map (pop-key :db :databases))
             (map (pop-key :migrator :migrators))
             (map (pop-key :seed :seeds))
             (remove nil?)
             seq)))

(defn- run-op [f targets args] (doseq [t targets] (apply f t args)))

(defn load-config [r]
  (edn/read {:readers {'env (fn [x] (System/getenv (str x)))
                       'envf (fn [[fmt & args]]
                               (apply format fmt
                                      (map #(System/getenv (str %)) args)))}}
            (PushbackReader. (io/reader r))))

(defn migrate [conf env & args]
  (require-joplin-ns conf)
  (if-let [targets (get-targets conf env (first args))]
    (run-op migrate-db targets (rest args))
    (println "No targets found")))

(defn seed [conf env & args]
  (require-joplin-ns conf)
  (if-let [targets (get-targets conf env (first args))]
    (run-op seed-db targets (rest args))
    (println "No targets found")))

(defn rollback [conf env database amount-or-id & args]
  (require-joplin-ns conf)
  (if-let [[target] (get-targets conf env database)]
    (apply rollback-db target amount-or-id args)
    (println "No targets found")))

(defn reset [conf env database & args]
  (require-joplin-ns conf)
  (if-let [[target] (get-targets conf env database)]
    (do
      (apply rollback-db target Integer/MAX_VALUE args)
      (apply migrate-db target args)
      (apply seed-db target args))
    (println "No targets found")))

(defn pending [conf env database & args]
  (require-joplin-ns conf)
  (if-let [targets (get-targets conf env database)]
    (run-op pending-migrations targets args)
    (println "No targets found")))

(defn create [conf env database id & args]
  (require-joplin-ns conf)
  (if-let [[target] (get-targets conf env database)]
    (apply create-migration target id args)
    (println "No targets found")))
