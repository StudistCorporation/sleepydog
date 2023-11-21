(ns sleepy.dog-test
  (:require [clojure.test :refer [deftest is testing]]
            [sleepy.dog :as datadog :refer [defn-traced
                                            http-headers
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

(deftest with-tracing-test
  (testing "transparent"
    (is (= 42 (with-tracing "addition-test"
                (+ 1 41)))))
  (testing "unpacks ExecutionException"
    ;; exceptions are wrapped ExecutionException when thrown through a future
    ;; with-tracing unpacks those
    (is (thrown? RuntimeException (with-tracing "async-throw-test"
                                    @(future
                                       (throw (RuntimeException. "foo"))))))))

(deftest http-headers-test
  (let [headers (http-headers)]
    (testing "trace-id inclusion"
      (let [trace (some-> (datadog/active-span!) (.context) (.toTraceId) (not-empty))]
        (is (= trace (get "x-datadog-trace-id" headers)))))
    (testing "parent-id inclusion"
      (let [trace (some-> (datadog/active-span!) (.context) (.toSpanId) (not-empty))]
        (is (= trace (get "x-datadog-parent-id" headers)))))))
