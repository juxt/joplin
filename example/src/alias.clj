(ns alias
  (:require [clojure.java.io :as io]
            [joplin.repl :as repl]))

(defn- url
  ([m]
   (url :urlf :url m))
  ([skey tkey m]
   (assoc m tkey (format (skey m) (:host m)))))

(defn load-config [path]
  (-> (io/resource path)
      repl/load-config
      (update-in [:databases :psql-dev] url)
      (update-in [:databases :dt-dev] url)
      (update-in [:databases :hive-dev] (partial url :subnamef :subname))

      ;; to test mem db
      (assoc-in [:databases :dt-dev :url] "datomic:mem://foo")
      ))

;; -------------------------------------------------------------------
;; lein aliases

(defn migrate [env & [db]]
  (let [conf (load-config "joplin.edn")]
    (if db
      (repl/migrate conf (keyword env) (keyword db))
      (repl/migrate conf (keyword env))))
  (System/exit 0))

(defn seed [env & [db]]
  (let [conf (load-config "joplin.edn")]
    (if db
      (repl/seed conf (keyword env) (keyword db))
      (repl/seed conf (keyword env))))
  (System/exit 0))

(defn rollback [env & [db num]]
  (let [conf (load-config "joplin.edn")]
    (when (and db num)
      (repl/rollback conf
                     (keyword env) (keyword db)
                     (Long/parseLong num))))
  (System/exit 0))

(defn reset [env & [db]]
  (let [conf (load-config "joplin.edn")]
    (when db
      (repl/reset conf (keyword env) (keyword db))))
  (System/exit 0))

(defn pending [env & [db]]
  (let [conf (load-config "joplin.edn")]
    (when db
      (repl/pending conf (keyword env) (keyword db))))
  (System/exit 0))

(defn create [env & [db id]]
  (let [conf (load-config "joplin.edn")]
    (when (and db id)
      (repl/create conf (keyword env) (keyword db) id)))
  (System/exit 0))
