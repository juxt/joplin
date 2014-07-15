(ns joplin.core
  (:require [clojure.java.io :as io]
            [ragtime.main]))

(defmulti migrate-db
  "Migrate target database described by a joplin database map."
  (fn [target & args] (get-in target [:db :type])))

(defmulti rollback-db
  "Rollback target database described by a joplin database map N steps
(N defaults to 1, and is optionally present in the args)."
  (fn [target & args] (get-in target [:db :type])))

(defmulti seed-db
  "Migrate target database described by a joplin database map."
  (fn [target & args] (get-in target [:db :type])))

(defmulti reset-db
  "Reset (re-migrate and seed) target database described by a joplin database map."
  (fn [target options & args] (get-in target [:db :type])))

(defmulti create-migration
  "Create migrations file(s) for target database described by a joplin database map."
  (fn [target options & args] (get-in target [:db :type])))

(defn- run-op [f targets args] (doseq [t targets] (apply f t args)))
(defn migrate [targets args] (run-op migrate-db targets args))
(defn rollback [targets args] (run-op rollback-db targets args))
(defn seed [targets args] (run-op seed-db targets args))
(defn reset [targets args] (run-op reset-db targets args))
(defn create [targets args] (run-op create-migration targets args))

(def verbose-migration @#'ragtime.main/verbose-migration)

(defn load-var [v]
  (try
    (@#'ragtime.main/load-var v)
    (catch Exception e
      (println (format "Function '%s' not found" v)))))

(defn- get-migration-ns [path]
  (let [ns (->> (clojure.string/split path #"/")
                rest
                (interpose ".")
                (apply str))
        folder (io/file path)]
    (->> (.listFiles folder)
         (map #(.getName %))
         (map #(re-matches #"(.*)(\.clj)$" %))
         (keep second)
         sort
         (map #(vector % (symbol (str ns "." %)))))))

(defn get-migrations [path]
  (for [[id ns] (get-migration-ns path)]
    (do
      (require ns)
      (verbose-migration
       {:id id
        :up (load-var (str ns "/up"))
        :down (load-var (str ns "/down"))}))))
