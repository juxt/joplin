(ns seeds.sql
  (:require [clojure.java.jdbc :as j]))

(defn run [target & args]
  (j/with-connection {:connection-uri (-> target :db :url)}
    (j/insert-values "test_table" [:id]
                     [0] [42])))
