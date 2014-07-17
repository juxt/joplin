(ns seeds.cass
  (:require [clojurewerkz.cassaforte.client :as cc]
            [clojurewerkz.cassaforte.cql    :as cql]
            [clojurewerkz.cassaforte.query  :as cq]
            [joplin.cassandra.database :refer [get-connection]]))

(defn run [target & args]
  (let [conn (get-connection (-> target :db :hosts)
                             (-> target :db :keyspace))]
    (cql/insert conn "users"
                {:id "Kalle" :email "ole@dole.doff"})))
