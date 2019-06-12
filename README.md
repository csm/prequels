# prequels

SQL [component](https://github.com/stuartsierra/component) that can load configurations from AWS RDS.


Can use [lore](https://github.com/csm/lore) to manage encrypted passwords.

## Usage

```clojure
[com.github.csm/prequels "0.1.0"]
```

```clojure
(require '[com.stuartsierra.component :refer [start system-map using]])
(require '[prequels.core :refer :all])

; static arg-map is just a java.jdbc database spec, with support for
; encrypted passwords.
(def static-spec (start (map->SQLConnection {:connection-type :static
                                             :arg-map {:dbtype "mysql"
                                                       :user "mysql"
                                                       :dbname "mydb"}})))

; use (:sql-spec static-spec) in clojure.java.jdbc calls

; RDS arg-map resolves the given RDS cluster ID.
(def system (start (system-map
                     :secret-store (lore.component/map->SecretStore {:store-type :kms})
                     :sql-conn (using
                                 (map->SQLConnection {:connection-type :rds
                                                      :arg-map {:cluster-id "my-rds-cluster"
                                                                :read-only? true
                                                                :encrypted-password "Base64EncodedData/=="
                                                                :aws {:region "us-west-2"}}})
                                 {:secret-store :secret-store}))))
```

You will need the [Cognitect AWS API](https://github.com/cognitect-labs/aws-api) in your dependencies to resolve RDS clusters.

You'll also likely want [java.jdbc](https://github.com/clojure/java.jdbc) and the appropriate
JDBC driver for your database.