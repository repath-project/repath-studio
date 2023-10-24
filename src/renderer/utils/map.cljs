(ns renderer.utils.map)

(defn deep-merge 
  [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn merge-common-with 
  [f a b]
  (merge-with f
              (select-keys a (keys b))
              (select-keys b (keys a))))