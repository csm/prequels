(ns prequels.impl.rds
  (:require [cognitect.aws.client.api :as aws]))

(defn ->sql-spec
  [{:keys [cluster-id read-only? rds-client aws db-name user password] :or {read-only? true}}]
  (let [rds-client (or rds-client (aws/client (assoc aws :api :rds)))
        result (aws/invoke rds-client {:op :DescribeDBClusters
                                       :request {:DBClusterIdentifier cluster-id}})]
    (if (some? (:cognitect.anomalies/category result))
      (throw (ex-info (str "failed to look up RDS cluster " (pr-str cluster-id)) {:result result}))
      {:dbtype (case (-> result :DBClusters first :Engine)
                 ("mysql" "aurora-mysql" "aurora") :mysql
                 ("postgresql" "aurora-postgresql") :postgresql)
       ; TODO, what are the other values for Engine?
       :dbname db-name
       :user user
       :password password
       :host (or (when read-only?
                   (-> result :DBClusters first :ReaderEndpoint))
                 (-> result :DBClusters first :Endpoint))
       :port (-> result :DBClusters first :Port)})))