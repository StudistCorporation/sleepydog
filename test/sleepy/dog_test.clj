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

(defn- trace-id-seen-by-handler
  "要求を wrap-ring-trace に通し、handler の中の span が外へ送る trace の ID。"
  [opts headers]
  (let [seen (atom nil)
        handler (wrap-ring-trace (fn [_]
                                   (reset! seen (get (http-headers) "x-datadog-trace-id"))
                                   {:status 200})
                                 opts)]
    (handler {:request-method :get :uri "/" :remote-addr "127.0.0.1" :headers headers})
    @seen))

(deftest wrap-ring-trace-test
  (testing "transparent"
    (is (= {:status 200} ((wrap-ring-trace (constantly {:status 200}))
                          {:request-method :get :uri "/" :remote-addr "127.0.0.1" :headers {}}))))
  ;; agent が無いと span は何もしない実装になり、trace の ID も無い
  (when (seq (with-tracing "probe" (http-headers)))
    (let [datadog-headers {"x-datadog-trace-id" "1234567890" "x-datadog-parent-id" "987654321"}]
      (testing "ignores incoming trace context by default"
        (is (not= "1234567890" (trace-id-seen-by-handler {} datadog-headers))))
      (testing "continues the trace that the caller sent when enabled"
        (is (= "1234567890" (trace-id-seen-by-handler {:continue-trace? true} datadog-headers)))
        (is (= (str (Long/parseLong "00f067aa0ba902b7" 16))
               (trace-id-seen-by-handler {:continue-trace? true}
                                         {"traceparent" "00-4bf92f3577b34da600f067aa0ba902b7-00f067aa0ba902b7-01"}))
            "W3C traceparent (the lower 64 bits of the trace id)"))
      (testing "starts a new trace without incoming context"
        (is (not= "1234567890" (trace-id-seen-by-handler {:continue-trace? true} {})))))))
