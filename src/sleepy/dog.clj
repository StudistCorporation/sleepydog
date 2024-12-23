(ns sleepy.dog
  (:require [sleepy.internal :as datadog :refer [*continuation*]])
  (:import [datadog.trace.api
            DDTags]
           [io.opentracing.util
            GlobalTracer]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

;; just aliases

(defn active-span!
  []
  (datadog/active-span!))

(defn set-resource!
  [reg-name]
  (datadog/set-resource! reg-name))

(defn report-error!
  [span ex]
  (datadog/report-error! span ex))

(defn root-of
  [span]
  (datadog/root-of span))

(defn http-headers
  []
  (let [span (active-span!)]
    (datadog/build-headers span)))

(defmacro with-tracing
  [op & body]
  `(let [inherited-scope# (when *continuation* (.activate *continuation*))
         tracer# (GlobalTracer/get)
         span# (-> (.buildSpan tracer# ~op)
                   (.withTag DDTags/LANGUAGE_TAG_KEY "clojure")
                   (.start))
         scope# (datadog/set-async! (.activateSpan tracer# span#))
         report!# (datadog/build-error-reporter span#)]
     (try
       (let [result# (binding [*continuation* (datadog/capture-scope scope#)]
                       ~@body)]
         (if (future? result#)
           (future
             (try
               @result#
               (catch Throwable ex#
                 (report!# ex#))
               (finally
                 (.finish span#)
                 (.close scope#)
                 (when inherited-scope# (.close inherited-scope#)))))
           result#))
       (catch Throwable ex#
         (report!# ex#))
       (finally
         (.finish span#)
         (.close scope#)
         (when inherited-scope# (.close inherited-scope#))))))

(defn wrap-ring-trace
  [handler]
  (fn ring-trace-wrapper
    [{:keys [remote-addr request-method uri]
      {:strs [content-length user-agent x-forwarded-for referer]} :headers
      :as request}]
    (with-tracing "ring.request"
      (let [span (active-span!)]
        ;; https://docs.datadoghq.com/tracing/trace_collection/tracing_naming_convention/#http-requests
        (datadog/tag-span! span "http.method" (name request-method))
        (datadog/tag-span! span "http.url" uri)
        (datadog/tag-span! span "http.useragent" user-agent)
        (datadog/tag-span! span "http.request.content_length" content-length)
        (datadog/tag-span! span "network.client.ip" (or x-forwarded-for remote-addr))
        (when referer
          (datadog/tag-span! span "http.referer" referer))
        (let [result (handler request)]
          (datadog/tag-span! span "http.status_code" (:status result))
          result)))))

(defmacro defn-traced
  [reg-name & code]
  (let [fname (str *ns* "/" reg-name)]
    `(defn ~reg-name
       ~@(loop [code code
                aggr []]
           (let [head (first code)]
             (cond
               (vector? head)
               (let [body (rest code)]
                 (if (and (map? (first body)) (not-empty (rest body)))
                   (conj aggr head (first body)
                         (concat (list `with-tracing fname) (rest body)))
                   (conj aggr head (concat (list `with-tracing fname) body))))

               (list? head)
               (let [args (first head)
                     body (rest head)]
                 (recur
                  (rest code)
                  (conj
                   aggr
                   (if (and (map? (first body)) (not-empty (rest body)))
                     (list args (first body)
                           (concat (list `with-tracing fname) (rest body)))
                     (list args (concat (list `with-tracing fname) body))))))

               (nil? head)
               aggr

               :else (recur (rest code) (conj aggr head))))))))
