(ns hooks
  (:require [clj-kondo.hooks-api :as api]))

(defn ^:export => [{:keys [node]}]
  (let [[macro-sym name-node & schema-nodes] (:children node)]
    {:node (api/list-node
            (list*
             macro-sym
             (with-meta name-node {:clj-kondo/ignore-reference true})
             schema-nodes))}))
