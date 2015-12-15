(ns migrators.dynamo.20151215114952-users
  (:use [joplin.dynamodb.database]
        [taoensso.faraday :as far]))

(defn up [db]
  (far/create-table db :users [:id :s]))

(defn down [db]
  (far/delete-table db :users))
