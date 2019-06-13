(ns prequels.impl.static
  (:require [next.jdbc :as jdbc]))

(defn ->sql-spec
  [arg-map]
  (jdbc/get-datasource arg-map))