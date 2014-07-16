(ns seeds.dt
  (:require [datomic.api :as d]))

(defn run [target & args]
  (let [conn (d/connect (-> target :db :url))]
    @(d/transact conn [{:db/id #db/id [:db.part/user -100]
                        :foobar/id 42}])))
