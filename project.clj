(defproject jp.studist/sleepydog "0.1.3"
  :description "Clojure library for tracing (possibly async) applications with Datadog."
  :url "https://github.com/StudistCorporation/sleepydog"
  :license {:name "MIT"
            :url "https://opensource.org/license/mit/"}
  :dependencies [[org.clojure/clojure "1.11.2"]
                 [com.datadoghq/dd-trace-ot "1.24.2"]]
  :deploy-repositories {"clojars" {:url "https://repo.clojars.org/"
                                   :username :env/clojars_user
                                   :password :env/clojars_token}}
  :profiles
  {:dev {:dependencies [[clj-kondo "2023.10.20"]
                        [lambdaisland/kaocha "1.87.1366"]]
         :plugins [[lein-ancient "0.7.0"]]
         :aliases {"lint" ["run" "-m" "clj-kondo.main"
                           "--config" ".clj-kondo/config.edn"
                           "--lint" "src" "test"]
                   "test" ["run" "-m" "kaocha.runner"]}}
   :agent {:jvm-opts ["-javaagent:./dd-java-agent.jar"]}})
