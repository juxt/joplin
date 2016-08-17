(defproject joplin.datomic "0.3.8"
  :description "Datomic support for Joplin"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [joplin.core "0.3.8"]
                 [com.datomic/datomic-free "0.9.5385" :exclusions [joda-time]]])
