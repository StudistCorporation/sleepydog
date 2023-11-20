(defproject jp.studist/sleepydog "0.1.0-alpha3"
  :description "FIXME: write description"
  :url "https://github.com/StudistCorporation/sleepydog"
  :license {:name "MIT"
            :url "https://opensource.org/license/mit/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.datadoghq/dd-trace-ot "1.17.0"]]
  :deploy-repositories {"clojars" {:url "https://repo.clojars.org/"
                                   :username :env/clojars_user
                                   :password :env/clojars_token}}
  :profiles
  {:dev {:dependencies [[clj-kondo "2023.10.20"]]
         :aliases {"lint" ["run" "-m" "clj-kondo.main"
                           "--config" ".clj-kondo/config.edn"
                           "--lint" "src" "test"]}}})
