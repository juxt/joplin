(ns migrators.es.20150205114807-users2
  (:use [joplin.elasticsearch.database]))

(defn up [db]
  (create-index (client db) "users2"
                :mappings {:pin
                           {:properties
                            {:location {:type "geo_point" :index "no"}}}})
  )

(defn down [db]
  (rollback-index (client db) "users2")
  )
