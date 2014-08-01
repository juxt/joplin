(ns migrators.cass.20140717174605-users
  (:use [joplin.cassandra.database])
  (:require [clojurewerkz.cassaforte.cql    :as cql]
            [clojurewerkz.cassaforte.query  :as cq]))

(defn up [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/create-table conn "users"
                      (cq/column-definitions {:id :varchar
                                              :email :varchar
                                              :primary-key [:id]}))))

(defn down [db]
  (let [conn (get-connection (:hosts db) (:keyspace db))]
    (cql/drop-table conn "users")))
