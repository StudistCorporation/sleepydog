# Sleepy Dog

[![Build Status](https://github.com/studistcorporation/sleepydog/actions/workflows/test.yaml/badge.svg?event=push)](https://github.com/studistcorporation/sleepydog/actions) [![Clojars Project](https://img.shields.io/clojars/v/jp.studist/sleepydog.svg)](https://clojars.org/jp.studist/sleepydog)

Clojure library for tracing (possibly async) applications with Datadog.

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
