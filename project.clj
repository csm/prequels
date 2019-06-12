(defproject com.github.csm/prequels "0.1.1-SNAPSHOT"
  :description "SQL server component"
  :url "https://github.com/csm/prequels"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [com.github.csm/lore "0.1.0"]]
  :profiles {:provided {:dependencies [[com.cognitect.aws/api "0.8.305"]
                                       [com.cognitect.aws/endpoints "1.1.11.565"]
                                       [com.cognitect.aws/rds "722.2.467.0"]]}
             :repl {:source-paths ["scripts"]
                    :dependencies [[com.cognitect.aws/api "0.8.305"]
                                   [com.cognitect.aws/endpoints "1.1.11.565"]
                                   [com.cognitect.aws/rds "722.2.467.0"]]}}
  :repl-options {:init-ns prequels.repl}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
