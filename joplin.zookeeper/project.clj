(defproject joplin.zookeeper "0.3.11-SNAPSHOT"
  :description "ZooKeeper support for Joplin"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [joplin.core "0.3.11-SNAPSHOT"]
                 [zookeeper-clj "0.9.4" :exclusions [org.apache.zookeeper/zookeeper log4j]]
                 [org.apache.zookeeper/zookeeper "3.4.8" :exclusions [commons-codec com.sun.jmx/jmxri
                                                                      com.sun.jdmk/jmxtools javax.jms/jms
                                                                      org.slf4j/slf4j-log4j12 log4j]]
                 [curator "0.0.6"]])
