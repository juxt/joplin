(ns seeds.dynamo
  (:require [taoensso.faraday :as far]))

(defn run [target & args]
  (far/put-item (:db target) :users
                {:id   "sven"
                 :name "Kristoff's horsie"
                 :age  22}))
