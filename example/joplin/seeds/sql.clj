(ns seeds.sql
  (:require [clojure.java.jdbc :as j]))

(defn run [target & args]
  (j/with-db-connection [db {:connection-uri (-> target :db :url)}]
    (j/insert! db :test_table {:id 0})
    (j/insert! db :test_table {:id 42})))
