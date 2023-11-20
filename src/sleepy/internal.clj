(ns sleepy.internal
  (:import [datadog.trace.api
            DDTags]
           [datadog.trace.api.interceptor
            MutableSpan]
           [datadog.trace.context
            TraceScope
            TraceScope$Continuation]
           [io.opentracing.log
            Fields]
           [io.opentracing.noop
            NoopSpan
            NoopScopeManager$NoopScope]
           [io.opentracing.util
            GlobalTracer]
           [java.util
            Collections]
           [java.util.concurrent
            ExecutionException]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;; utilizing Clojure binding to pass state across threads
(def ^:dynamic ^TraceScope$Continuation *continuation*
  nil)

(defprotocol Tag
  (add-tag! [v span k]))

;; MutableSpanの３種類のoverloadされてるsetTagへのreflection warningを解消するためのやつ
;; https://github.com/DataDog/dd-trace-java/blob/8a8511f2e451ebb9cb3468d7bbaeb6160afdb1e2/dd-trace-api/src/main/java/datadog/trace/api/interceptor/MutableSpan.java#L48-L52
(extend-protocol Tag
  String
  (add-tag!
   [v ^MutableSpan span ^String k]
   (.setTag span k v))
  Boolean
  (add-tag!
   [v ^MutableSpan span ^String k]
   (.setTag span k v))
  Number
  (add-tag!
   [v ^MutableSpan span ^String k]
   (.setTag span k v))
  nil
  (add-tag!
   [_ _ _]))

(defn active-span!
  []
  (let [tracer (GlobalTracer/get)]
    (when *continuation* (.activate *continuation*))
    (.activeSpan tracer)))

;; agentが無効だとNoopSpanになるので何もしない実装も準備
(defprotocol Span
  (tag-span! [span k v])
  (root-of [span])
  (report-error! [span ex]))

(extend-protocol Span
  MutableSpan
  (tag-span!
   [span k v]
   (add-tag! v span k))
  (root-of
   [span]
   (.getLocalRootSpan span))
  (report-error!
   [span ex]
   (.setError span true)
   ;; MutableSpanで拾ってるが、OpenTracingのSpanのinterfaceも実装していてそっちのlogを使う
   (.log ^io.opentracing.Span span (Collections/singletonMap Fields/ERROR_OBJECT ex)))

  NoopSpan
  (tag-span!
   [_ _ _])
  (root-of
   [_])
  (report-error!
   [_ _]))

(defn set-resource!
  [^String reg-name]
  (let [tracer (GlobalTracer/get)]
    (when-let [span (.activeSpan tracer)]
      (tag-span! span DDTags/RESOURCE_NAME reg-name))))

(defprotocol Scope
  (capture-scope ^TraceScope$Continuation [scope])
  (set-async! ^io.opentracing.Scope [scope]))

(extend-protocol Scope
  TraceScope
  (capture-scope
   [scope]
   (.captureConcurrent scope))
  (set-async!
   [scope]
   (doto scope (.setAsyncPropagation true)))

  NoopScopeManager$NoopScope
  (capture-scope
   [_]
   nil)
  (set-async!
   [scope]
   scope))

(defprotocol DDHeaders
  (build-headers [span]))

(extend-protocol DDHeaders
  io.opentracing.Span
  (build-headers
   [span]
   (let [context (.context span)]
     ;; ローカル環境で観測したDD周りのヘダーを再現
     {"x-datadog-sampling-priority" 1
      "x-datadog-tags" "_dd.p.dm=-1"
      "x-datadog-trace-id" (.toTraceId context)
      "x-datadog-parent-id" (.toSpanId context)}))

  NoopSpan
  (build-headers
   [_]
   {}))

(defprotocol AsyncCatchable
  (report-error [ex span]))

(extend-protocol AsyncCatchable
  ExecutionException
  (report-error
   [ex span]
   (if-let [cause (.getCause ex)]
     (do
       (report-error! span cause)
       (throw cause))
     (throw ex)))

  Throwable
  (report-error
   [ex span]
   (report-error! span ex)
   (throw ex)))

(defn build-error-reporter
  [span]
  (fn report-span-error
    [ex]
    (report-error ex span)))
