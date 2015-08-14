(ns migrators.jdbc.20141019131642-add-test-table
  (:require [clojure.java.jdbc :as jdbc]))

(defn up [db]
  (jdbc/execute! (:db-spec db)
    (jdbc/create-table-ddl "test_table"
                           [:id "varchar(255)"])))

(defn down [db]
  (jdbc/execute! (:db-spec db)
    (jdbc/drop-table-ddl :test_table)))
