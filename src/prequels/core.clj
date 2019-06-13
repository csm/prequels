(ns prequels.core
  "Component wrapping a SQL connection, for usage in org.clojure/java.jdbc.

  Argument reference for SQLConnection:

  * `:secret-store` A value that satisfies `lore.api.async/IAsyncSecretStore`. Typically
    this is another component (e.g. `lore.component/SecretStore`). Only used if the password
    is encrypted in `arg-map`.
  * `:connection-type` A keyword, either `:static` or `:rds`.
  * `:arg-map` A map of arguments.

  For `:static` connection types, `arg-map` should be a java.jdbc DB spec map, but
  *may* also contain a key `:encrypted-password` instead of `:password`. If no `:password`
  is given but `:encrypted-password` is, it should be a base-64 encoded string, byte
  array, or input stream containing the encrypted password. On start, the encrypted
  password is decrypted via the secret-store.

  For `:rds` connection types, the `:encrypted-password` key is handled as above, and
  the other arguments are:

  * `:cluster-id` The name of the RDS cluster to look up (required).
  * `:dbname` The DB name to connect to.
  * `:user` The database user to authenticate with.
  * `:read-only?` A boolean, if true, fetch the read-only endpoint of the cluster,
    if available. Otherwise fetches the main endpoint. Default true.
  * `aws` Argument map to pass to `cognitect.aws.client.api/client`, to override
    any defaults.
  * `rds-client` to pass an existing AWS client instance.
  * `expires` a java.time.Duration, java.time.Period, long (milliseconds) or
    string (ISO duration string); how long to cache RDS results before fetching
    the config again. Default is to never expire."
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [lore.api.async :as lore]
            [lore.util :refer [dynacall]]
            prequels.impl.static))

(defrecord SQLConnection [secret-store connection-type arg-map db]
  component/Lifecycle
  (start [this]
    (let [arg-map (if (some? (:password arg-map))
                    arg-map
                    (if-let [encrypted-password (:encrypted-password arg-map)]
                      (let [result (async/<!! (lore/decrypt secret-store encrypted-password))]
                        (if (lore/error? secret-store result)
                          (throw (ex-info "failed to decrypt password" {:result result}))
                          (assoc arg-map :password (String. ^"[B" (:plaintext result)))))
                      arg-map))
          data-source (case connection-type
                        :rds (dynacall 'com.cognitect.aws/rds
                                       'prequels.impl.rds/->rds-data-source
                                       arg-map)
                        :static (prequels.impl.static/->sql-spec arg-map))]
      (assoc this :db data-source)))

  (stop [this] this))
