(ns seeds.es
  (:require [clojurewerkz.elastisch.rest.document :as esd]
            [joplin.elasticsearch.database :refer [client native-client migrate-data-native]]))

(defn run [target & args]
  (esd/put (client (:db target)) "users" "user" "0"
           {:name "Foo Bar"
            :email "lol@lol"}))
