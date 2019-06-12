(in-ns 'prequels.repl)

(require '[cognitect.aws.client.api :as aws])

(def rds (aws/client {:api :rds}))

(def ops (aws/ops rds))
(keys ops)
(:DescribeDBClusters ops)
(def db-cluster-id "...")
(aws/invoke rds {:op :DescribeDBClusters :request {:DBClusterIdentifier db-cluster-id}})

(require '[prequels.core :as p])
(require '[lore.component :as lore])
(require '[com.stuartsierra.component :refer [start system-map using]])

(def db-name "...")
(def db-user "...")
(def encrypted-password "...")
(def system (start (system-map
                     :secret-store (lore/map->SecretStore {:store-type :kms})
                     :sql (using (p/map->SQLConnection {:connection-type :rds
                                                        :arg-map {:cluster-id db-cluster-id
                                                                  :encrypted-password encrypted-password
                                                                  :db-name db-name
                                                                  :user db-user}})
                                 {:secret-store :secret-store}))))