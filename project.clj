(defproject jp.studist/sleepydog "0.3.0"
  :description "Clojure library for tracing (possibly async) applications with Datadog."
  :url "https://github.com/StudistCorporation/sleepydog"
  :license {:name "MIT"
            :url "https://opensource.org/license/mit/"}
  :dependencies [[org.clojure/clojure "1.12.4"]
                 [com.datadoghq/dd-trace-ot "1.58.0"]]
  :scm {:name "git"
        :tag "v0.3.0"}
  :deploy-repositories {"clojars" {:url "https://repo.clojars.org/"
                                   :username :env/clojars_user
                                   :password :env/clojars_token}}
  :profiles
  {:dev {:dependencies [[clj-kondo "2026.01.12"]
                        [lambdaisland/kaocha "1.91.1392"]
                        [com.taoensso/carmine "3.5.0"]]
         :plugins [[lein-ancient "0.7.0"]]
         :aliases {"lint" ["run" "-m" "clj-kondo.main"
                           "--config" ".clj-kondo/config.edn"
                           "--lint" "src" "test"]
                   "test" ["run" "-m" "kaocha.runner"]}}
   :agent {:jvm-opts ["-javaagent:./dd-java-agent.jar"]}})
