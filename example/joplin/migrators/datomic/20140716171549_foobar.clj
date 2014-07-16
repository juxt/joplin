(ns migrators.datomic.20140716171549-foobar
  (:use [joplin.datomic.database])
  (:require [datomic.api :as d]))

(defn up [db]
  (let [conn (d/connect (:url db))]
    (transact-schema conn [{:db/ident :foobar/id
                            :db/valueType :db.type/long
                            :db/cardinality :db.cardinality/one
                            :db/id #db/id [:db.part/db]
                            :db.install/_attribute :db.part/db}])))

(defn down [db]
  ;; Can't rmeove attributes in datomic so down-migrations is a bit naff
  )
