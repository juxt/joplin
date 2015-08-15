(ns migrators.jdbc.20141019131642-add-test-table
  (:use [clojure.java.jdbc.deprecated :as sql]))

(defn up [db]
  (sql/with-connection db
    (sql/create-table :test_table
                      [:id "INT"])))

(defn down [db]
  (sql/with-connection db
    (sql/drop-table :test_table)))
