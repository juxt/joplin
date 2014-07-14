(ns joplin.zookeeper.database
  (:use [joplin.core]))

(defmethod migrate-db :zk [target & args]
  (println ":zk" target))
