(ns migrators.es.20140715163523-user-mappings
  (:use [joplin.elasticsearch.database]))

(defn up [db]
  (create-index (client db) "users"
                :mappings {:pin
                           {:properties
                            {:location {:type "geo_point" :index "no"}}}})
  )

(defn down [db]
  (rollback-index (client db) "users")
  )
