(ns joplin.core)

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

(defn- run-op [f targets args] (doseq [t targets] (apply f t args)))
(defn migrate [targets args] (run-op migrate-db targets args))
(defn rollback [targets args] (run-op rollback-db targets args))
(defn seed [targets args] (run-op seed-db targets args))
(defn reset [targets args] (run-op reset-db targets args))
