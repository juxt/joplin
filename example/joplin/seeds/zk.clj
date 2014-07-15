(ns seeds.zk
  (:require [joplin.zookeeper.database :as zk])
  )

(defn run [target & args]
  (println "ZK" target args)
  (let [client (zk/connect target)]
    (zk/write-data client "/foo" {:a 1 :b 2})
    (println (zk/read-data client "/foo"))
    (zk/delete-data client "/foo")
    (zk/close client))

  )
