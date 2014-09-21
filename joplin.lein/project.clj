(defproject joplin.lein "0.1.12"
  :description "Joplin Leiningen plugin"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[leinjacker "0.4.1"]]

  :eval-in-leiningen false    ;; The lucene clash (for elasticsearch)
  )
