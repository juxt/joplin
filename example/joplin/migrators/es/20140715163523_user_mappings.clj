(ns migrators.es.20140715163523-user-mappings
  (:use [joplin.elasticsearch.database]))

(defn up [target & args]
  (create-index "users"
                :mappings {:pin
                           {:properties
                            {:location {:type "geo_point" :index "no"}}}})
  )

(defn down [target & args]
  (rollback-index "users")
  )
