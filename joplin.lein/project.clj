(defproject joplin.lein "0.1.4-SNAPSHOT"
  :description "Flexible datastore migrations and seeds"
  :url "http://github.com/martintrojer/joplin"
  :scm {:name "git"
        :url "https://github.com/martintrojer/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[leinjacker "0.4.1"]]

  :eval-in-leiningen false    ;; The lucene clash (for elasticsearch)
  )
