(ns migrators.es.20150205180539-move-data
  (:use [joplin.elasticsearch.database]))

(defn up [db]
  (migrate-data-native
   (client db)
   (native-client (assoc db :port 9300))
   "users" "user" "users2")
  )

(defn down [db]
  ;; TODO - down migration goes here
  )
