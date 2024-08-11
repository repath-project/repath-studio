(ns renderer.utils.map)

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn deep-merge
  [a & more]
  (if (map? a)
    (apply merge-with deep-merge a more)
    (apply merge-with deep-merge more)))

(defn merge-common-with
  [f a b]
  (merge-with f
              (select-keys a (keys b))
              (select-keys b (keys a))))
