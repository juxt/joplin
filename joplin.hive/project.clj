(defproject joplin.hive "0.3.4"
  :description "Hive Avro support for Joplin"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [joplin.core "0.3.4"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [org.apache.hadoop/hadoop-client "2.7.1"]
                 [org.apache.hive/hive-exec "1.2.1"]
                 [org.apache.hive/hive-jdbc "1.2.1"]
                 [commons-io/commons-io "2.4"]])
