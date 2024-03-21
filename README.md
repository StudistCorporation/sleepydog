# Sleepy Dog

[![Build Status](https://github.com/studistcorporation/sleepydog/actions/workflows/test.yaml/badge.svg?event=push)](https://github.com/studistcorporation/sleepydog/actions) [![Clojars Project](https://img.shields.io/clojars/v/jp.studist/sleepydog.svg)](https://clojars.org/jp.studist/sleepydog)

Clojure library for tracing (possibly async) applications with Datadog.

## Installation

Available on Clojars.

```clj
[jp.studist/sleepydog "0.2.0"]
```

## Usage

The public API is in the `sleepy.dog` namespace.

### `defn-traced`

A drop-in replacement for Clojure's `defn` that instruments the function in question as a new Datadog span.

### `wrap-ring-trace`

A ring middleware that automatically instruments server requests.

### `with-tracing "op-name"`

A macro that instruments the given body as a span labelled as operation "op-name".

### `set-resource!`

Sets the resource (subject) of the current span's operation.

```clj
(with-tracing "s3-upload"
  (set-resource! "foo/bar/baz.jpg")
  ,,, ; prepare upload
  (.putObject client put-request))
```

### `http-headers`

Utility function that builds Datadog headers for distributed tracing. By adding these headers to any outgoing (upstream) request, it's possible to connect traces across services.

```clj
;; (:require [sleepy.dog :as datadog])
(merge (datadog/http-headers) headers)
```

### `report-error!`

Used to manually report caught exceptions (without relying on the automatic reporting from `with-tracing` or `defn-traced`). This can be useful for example in a Ring handler that catches all escaped exceptions and responds with a well-formed 500 error.

If you want to mark an entire request as "errored", then combine this with `root-of` to mark the root span as error.

```clj
(defn wrap-exception
  [handler]
  (fn exception-catcher
    [request]
    (try
      (handler request)
      (catch Throwable ex
        (let [span (datadog/active-span!)]
          (datadog/report-error! span ex)
          (when-let [root (datadog/root-of span)]
            (datadog/report-error! root ex)))
        {:status 500 :body (.getMessage ex)}))))
```

### Tracing Carmine

You can enable tracing for the [Carmine Redis library](https://github.com/taoensso/carmine) by using `sleepy.dog.carmine/trace-carmine!`. The traces will be reported mimicking the way how Datadog auto-instruments Jedis or the Ruby redis gem.
