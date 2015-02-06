(ns migrators.esdata.20150205180501-change-user
  (:use [clojurewerkz.elastisch.rest.document :as esd]
        [joplin.elasticsearch.database]))

(defn up [db]
  (esd/put (client db) "users" "user" "0"
           {:name "Foo Bar BAZ"
            :email "lol@lol.baz"})
  )

(defn down [db]
  (esd/put (client db) "users" "user" "0"
           {:name "Foo Bar"
            :email "lol@lol"})
  )
