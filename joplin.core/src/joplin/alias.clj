(ns joplin.alias
  (:require [clojure.java.io :as io]
            [joplin.repl :as repl]))

(defn- load-config' [path]
  (-> (io/resource path)
      repl/load-config))

(def ^:dynamic *load-config* load-config')

;; -------------------------------------------------------------------
;; lein aliases

;; config file is the filename of a joplin configuration which has to recide
;; somewhere in the resources.

(defn migrate [config-file env & [db]]
  (let [conf (*load-config* config-file)]
    (if db
      (repl/migrate conf (keyword env) (keyword db))
      (repl/migrate conf (keyword env))))
  (System/exit 0))

(defn seed [config-file env & [db]]
  (let [conf (*load-config* config-file)]
    (if db
      (repl/seed conf (keyword env) (keyword db))
      (repl/seed conf (keyword env))))
  (System/exit 0))

(defn rollback [config-file env & [db num]]
  (let [conf (*load-config* config-file)]
    (when (and db num)
      (repl/rollback conf
                     (keyword env) (keyword db)
                     (Long/parseLong num))))
  (System/exit 0))

(defn reset [config-file env & [db]]
  (let [conf (*load-config* config-file)]
    (when db
      (repl/reset conf (keyword env) (keyword db))))
  (System/exit 0))

(defn pending [config-file env & [db]]
  (let [conf (*load-config* config-file)]
    (when db
      (repl/pending conf (keyword env) (keyword db))))
  (System/exit 0))

(defn create [config-file env & [db id]]
  (let [conf (*load-config* config-file)]
    (when (and db id)
      (repl/create conf (keyword env) (keyword db) id)))
  (System/exit 0))
