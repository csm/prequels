(in-ns 'prequels.repl)

(require '[cognitect.aws.client.api :as aws])

(def rds (aws/client {:api :rds}))

(def ops (aws/ops rds))
(keys ops)
(:DescribeDBClusters ops)
(def db-cluster-id "...")
(aws/invoke rds {:op :DescribeDBClusters :request {:DBClusterIdentifier db-cluster-id}})