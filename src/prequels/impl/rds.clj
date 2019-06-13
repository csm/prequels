(ns prequels.impl.rds
  (:require [cognitect.aws.client.api :as aws]
            [clojure.java.io :as io]
            [java-time :as t]
            [next.jdbc :as jdbc])
  (:import [java.time Instant]
           [javax.sql DataSource]))

(defn ->sql-spec
  [{:keys [cluster-id read-only? rds-client aws dbname user password] :or {read-only? true}}]
  (let [rds-client (or rds-client (aws/client (assoc aws :api :rds)))
        result (aws/invoke rds-client {:op :DescribeDBClusters
                                       :request {:DBClusterIdentifier cluster-id}})]
    (if (some? (:cognitect.anomalies/category result))
      (throw (ex-info (str "failed to look up RDS cluster " (pr-str cluster-id)) {:result result}))
      {:dbtype                      (case (-> result :DBClusters first :Engine)
                                      ("mysql" "aurora-mysql" "aurora") "mysql"
                                      ("postgresql" "aurora-postgresql") "postgresql")
       ; TODO, what are the other values for Engine?
       :dbname                      dbname
       :user                        user
       :password                    password
       :host                        (or (when read-only?
                                          (-> result :DBClusters first :ReaderEndpoint))
                                        (-> result :DBClusters first :Endpoint))
       :port                        (-> result :DBClusters first :Port)
       :trustCertificateKeyStoreUrl (str (io/resource "com.github.csm.prequels.truststore"))
       :trustCertificateKeyStorePassword "com.github.csm/prequels"})))

(defn ->rds-data-source
  [{:keys [expires]
    :or {expires (t/minutes 5)}
    :as args}]
  (let [cache (atom nil)
        get-ds (fn get-ds []
                 (swap! cache (fn [ds]
                                (if (or (nil? ds)
                                        (t/after? (t/instant) (:expires ds)))
                                  {:datasource (jdbc/get-datasource (->sql-spec args))
                                   :expires (t/plus (t/instant) expires)}
                                  ds))))]
    (reify DataSource
      (getConnection [this]
        (.getConnection ^DataSource (:datasource (get-ds))))
      (getConnection [this user password]
        (.getConnection ^DataSource (:datasource (get-ds)) user password))
      (getLogWriter [this]
        (.getLogWriter ^DataSource (:datasource (get-ds))))
      (setLogWriter [this w]
        (.setLogWriter ^DataSource (:datasource (get-ds)) w))
      (setLoginTimeout [this t]
        (.setLoginTimeout ^DataSource (:datasource (get-ds)) t))
      (getLoginTimeout [this]
        (.getLoginTimeout ^DataSource (:datasource (get-ds))))
      (getParentLogger [this]
        (.getParentLogger ^DataSource (:datasource (get-ds))))
      (unwrap [this iface]
        (.unwrap ^DataSource (:datasource (get-ds)) iface))
      (isWrapperFor [this iface]
        (.isWrapperFor ^DataSource (:datasource (get-ds)) iface)))))