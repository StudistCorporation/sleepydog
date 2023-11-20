# Sleepy Dog

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

## License

Copyright © 2023 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
