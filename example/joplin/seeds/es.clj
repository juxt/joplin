(ns seeds.es
  (:require [clojurewerkz.elastisch.native.document :as esd]
            [joplin.elasticsearch.database :refer [*es-client*]]))

(defn run [target & args]
  (esd/put *es-client* "users" "user" "0"
           {:name "Foo Bar"
            :email "lol@lol"}))
