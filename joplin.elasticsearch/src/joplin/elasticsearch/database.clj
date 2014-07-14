(ns joplin.elasticsearch.database
  (:use [joplin.core]))

(defmethod migrate-db :es [target & args]
  (println ":es" target))
