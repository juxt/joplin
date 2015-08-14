(ns migrators.jdbc.extrapath.0001
  (:require [clojure.java.jdbc :as sql]))

(defn up [db]
  (sql/execute! (:db-spec db)
    (sql/create-table-ddl :test_table_2
                          [:id "INT"])))

(defn down [db]
  (sql/execute! (:db-spec db)
    (sql/drop-table-ddl :test_table_2)))
