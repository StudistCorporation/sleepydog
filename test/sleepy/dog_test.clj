(ns sleepy.dog-test
  (:require [clojure.test :refer [deftest is testing]]
            [sleepy.dog :as datadog :refer [defn-traced
                                            http-headers
                                            with-tracing
                                            wrap-ring-trace]]))

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
      {:value 42})
    (is (= {:value 42} (hoge)))))

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

(defn- trace-id-in-handler
  [opts headers]
  (let [handler (wrap-ring-trace (fn [_] (get (http-headers) "x-datadog-trace-id")) opts)]
    (handler {:request-method :get :uri "/" :remote-addr "127.0.0.1" :headers headers})))

(deftest wrap-ring-trace-test
  (let [incoming {"x-datadog-trace-id" "1234567890" "x-datadog-parent-id" "987654321"}]
    (is (not= "1234567890" (trace-id-in-handler nil incoming)))
    ;; without the agent there is no trace id to compare
    (when (seq (with-tracing "probe" (http-headers)))
      (is (= "1234567890" (trace-id-in-handler {:continue-trace? true} incoming)))
      (is (= (str 0x00f067aa0ba902b7)
             (trace-id-in-handler {:continue-trace? true}
                                  {"traceparent" "00-4bf92f3577b34da600f067aa0ba902b7-b7ad6b7169203331-01"}))))))
