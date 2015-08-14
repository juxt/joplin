(ns migrators.jdbc.20141019131642-add-test-table
  (:use [clojure.java.jdbc :as jdbc]
        [joplin.jdbc.database]))

(defn up [db]
  (sql/execute! (:db-spec db)
    (sql/create-table-ddl :test_table
                          [:id "INT"])))

(defn down [db]
  (sql/execute! (:db-spec db)
    (sql/drop-table-ddl :test_table)))
