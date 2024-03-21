(ns sleepy.dog.carmine-test
  (:require [clojure.test :refer [deftest is testing]]
            [taoensso.carmine :as carmine]
            [sleepy.dog.carmine :as carmine-dog]))

(deftest trace-carmine-test
  (testing "carmine vars are instrumented"
    (carmine-dog/trace-carmine!)
    (is (true? (:sleepy.dog/traced? (meta carmine/get)))))
  (testing "The instrumentation call is idempotent"
    (carmine-dog/trace-carmine!)
    (is (true? (:sleepy.dog/traced? (meta carmine/get))))
    (is (fn? carmine/get))))
