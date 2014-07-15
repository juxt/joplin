(ns joplin.zookeeper.exhibitor
  (:require [curator.framework :refer [exponential-retry]])
  (:import [org.apache.curator.ensemble.exhibitor
            Exhibitors
            ExhibitorEnsembleProvider
            Exhibitors$BackupConnectionStringProvider
            DefaultExhibitorRestClient]
           [org.apache.curator.retry
            RetryNTimes]
           [org.apache.curator.retry ExponentialBackoffRetry]
           [org.apache.curator.framework CuratorFramework CuratorFrameworkFactory]
           [org.apache.curator.framework.imps CuratorFrameworkState]
           [java.util.concurrent TimeUnit]))

(defn- ensemble-provider [host port]
  (ExhibitorEnsembleProvider.
   (Exhibitors. [host] port
                (proxy [Exhibitors$BackupConnectionStringProvider] []
                  (getBackupConnectionString []
                    (str host ":" port))))
   (DefaultExhibitorRestClient.)
   "/exhibitor/v1/cluster/list"
   5000 ;; Poll every
   (RetryNTimes. 10 1000)))

(defn exhibitor-framework [host port]
  (-> (doto (CuratorFrameworkFactory/builder)
        (.ensembleProvider (ensemble-provider host port))
        (.retryPolicy (exponential-retry 1000 10))
        (.connectionTimeoutMs 500)
        (.sessionTimeoutMs (* 40 1000)))
      (.build)))
