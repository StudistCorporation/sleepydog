(ns sleepy.dog.carmine
  (:require [clojure.string :as str]
            [sleepy.dog :refer [with-tracing]]
            [sleepy.internal :as datadog]))

(defn trace-carmine!
  "This depends on carmine-side metadata.

   If you use Redis on the hot path in a very performance-sensitive environment,
   think twice before you instrument all the Redis calls using this function."
  []
  (let [redis-vars
        (into {}
              (comp
               (filter (fn [[_ v]] (-> v meta :redis-api)))
               (map (fn [[k v]] [v (-> k name str/upper-case (str/replace #"-" " "))])))
              (ns-publics 'taoensso.carmine))]
    (doseq [[redis-var op] redis-vars]
      (alter-var-root
       redis-var
       (fn var-updater
         [original-fn]
         ;; tag functions as already-traced so this becomes idempotent and reload-friendly
         (let [old-meta (meta original-fn)]
           (if-not (:sleepy.dog/traced? old-meta)
             (with-meta
               (fn [& args]
                 (with-tracing "redis.command"
                   (datadog/set-resource! op)
                   (when-let [span (datadog/active-span!)]
                     (datadog/tag-span! span "service" "redis")
                     (datadog/tag-span! span "db.operation" op)
                     (datadog/tag-span! span "db.system" "redis"))
                   (apply original-fn args)))
               (assoc old-meta :sleepy.dog/traced? true))
             original-fn)))))))
