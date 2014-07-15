(ns leiningen.joplin
  (:require [leinjacker.deps :as deps])
  (:use [leiningen.run :only (run)]))

(defn- add-joplin-deps [project]
  (-> project
      (deps/add-if-missing '[joplin.core "0.1.3-SNAPSHOT"])
      ;; TODO -- leave out
      (deps/add-if-missing '[joplin.jdbc "0.1.3-SNAPSHOT"])
      (deps/add-if-missing '[joplin.elasticsearch "0.1.3-SNAPSHOT"])
      (deps/add-if-missing '[joplin.zookeeper "0.1.3-SNAPSHOT"])))

(defn joplin
  "Migrate and seed datastores"
  [project command & args]
  (let [environments (-> project :joplin :environments)
        databases    (-> project :joplin :databases)
        migrators    (-> project :joplin :migrators)
        seeds        (-> project :joplin :seeds)
        project      (add-joplin-deps project)]
    (apply run project
           "-m" "joplin.main"
           "-r" "joplin.jdbc.database,joplin.elasticsearch.database,joplin.zookeeper.database"
           "-e" environments
           "-d" databases
           "-m" migrators
           "-s" seeds
           command args)))
