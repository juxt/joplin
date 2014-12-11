(ns migrators.hive.20141211091054-test
  (:use [joplin.hive.database])
  (:require [clojure.java.jdbc :as jdbc]))

(defn up [db]
  (->> "create table test (id STRING)"
       (jdbc/db-do-prepared (get-spec (:subname db)) false))
  )

(defn down [db]
  ;; TODO - down migration goes here
  )
