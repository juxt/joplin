(defproject joplin.hive "0.3.11-SNAPSHOT"
  :description "Hive Avro support for Joplin"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [joplin.core "0.3.11-SNAPSHOT"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [org.apache.hadoop/hadoop-client "2.7.2"]
                 [org.apache.hive/hive-exec "2.1.0"]
                 [org.apache.hive/hive-jdbc "2.1.0"]
                 [commons-io/commons-io "2.5"]])
