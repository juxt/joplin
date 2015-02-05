(ns seeds.es
  (:require [clojurewerkz.elastisch.rest.document :as esd]
            [joplin.elasticsearch.database :refer [client native-client migrate-data-native]]))

(defn run [target & args]
  (esd/put (client (:db target)) "users" "user" "0"
           {:name "Foo Bar"
            :email "lol@lol"})

  (migrate-data-native
   (client (:db target))
   (native-client {:host (get-in target [:db :host])
                   :port 9300})
   "users" "user" "users2"))
