(defproject joplin.zookeeper "0.3.3"
  :description "ZooKeeper support for Joplin"
  :url "http://github.com/juxt/joplin"
  :scm {:name "git"
        :url "https://github.com/juxt/joplin"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [joplin.core "0.3.3"]
                 [zookeeper-clj "0.9.3" :exclusions [org.apache.zookeeper/zookeeper log4j]]
                 [org.apache.zookeeper/zookeeper "3.4.6" :exclusions [commons-codec com.sun.jmx/jmxri
                                                                      com.sun.jdmk/jmxtools javax.jms/jms
                                                                      org.slf4j/slf4j-log4j12 log4j]]
                 [curator "0.0.6"]])
