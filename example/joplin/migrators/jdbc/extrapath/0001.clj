(ns migrators.jdbc.extrapath.0001
  (:use [clojure.java.jdbc :as sql]
        [joplin.jdbc.database]))

(defn up [db]
  (sql/with-connection db
    (sql/create-table :test_table_2
                      [:id "INT"])))

(defn down [db]
  (sql/with-connection db
    (sql/drop-table :test_table_2)))
