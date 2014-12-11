(defproject joplin.hive "0.2.3"
  :description "Hive Avro support for Joplin"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [org.apache.hadoop/hadoop-client "2.4.0"]
                 [org.apache.hive/hive-exec "0.13.0"]
                 [org.apache.hive/hive-jdbc "0.13.0"]
                 [commons-io/commons-io "2.4"]
                 [joplin.core "0.2.3"]])
