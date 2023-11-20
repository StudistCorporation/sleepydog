(ns sleepy.dog-test
  (:require [clojure.test :refer [deftest is testing]]
            [sleepy.dog :refer [defn-traced
                                ;; http-headers
                                ;; set-resource!
                                with-tracing
                                ;; wrap-ring-trace
                                ]]))

(deftest defn-traced-test
  (testing "macro expansion"
    (let [expected
          (list
           `defn 'foo
           []
             ;; tests seem to count as if executed in user ns...
           (list `with-tracing "user/foo"
                 true))
          actual
          (macroexpand-1 '(sleepy.dog/defn-traced foo [] true))]
      (is (= expected actual)))
    (let [expected
          (list `defn 'foo
                "docstring"
                (list [] (list `with-tracing "user/foo" (list 'foo 1)))
                (list ['x] (list `with-tracing "user/foo" (list `dec 'x))))
          actual
          (macroexpand-1 '(sleepy.dog/defn-traced foo
                            "docstring"
                            ([] (foo 1))
                            ([x] (clojure.core/dec x))))]
      (is (= expected actual))))
  (testing "really callable"
    #_{:clj-kondo/ignore [:inline-def]}
    (defn-traced hoge
      []
      42)
    (is (= 42 (hoge)))))
