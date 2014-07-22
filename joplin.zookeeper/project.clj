(defproject joplin.zookeeper "0.1.6"
  :description "ZooKeeper support for Joplin"
  :url "http://github.com/martintrojer/joplin"
  :scm {:name "git"
        :url "https://github.com/martintrojer/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [zookeeper-clj "0.9.1" :exclusions [org.apache.zookeeper/zookeeper log4j]]
                 [org.apache.zookeeper/zookeeper "3.4.5" :exclusions [commons-codec com.sun.jmx/jmxri
                                                                      com.sun.jdmk/jmxtools javax.jms/jms
                                                                      org.slf4j/slf4j-log4j12 log4j]]
                 [joplin.core "0.1.6"]
                 [curator "0.0.2"]])
