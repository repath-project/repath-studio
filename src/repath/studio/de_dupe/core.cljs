(ns repath.studio.de-dupe.core
  (:require [clojure.walk :refer [postwalk-replace]]))

(defn side-walk
  "Traverses form, an arbitrary data structure.  inner and outer are
  functions.  Applies inner to each element of form, building up a
  data structure of the same type, then applies outer to the result.
  Recognizes all Clojure data structures. Consumes seqs as with doall."
  
  {:added "1.1"}
  [inner outer form]
  (cond
    (list? form) (outer form (apply list (doall (map inner form))))
    (satisfies? IMapEntry form) (outer form (vec (doall (map inner form))))
    (seq? form) (outer form (doall (map inner form)))
    (satisfies? IRecord form) (outer form (reduce (fn [r x] (conj r (inner x))) form form))
    (coll? form) (outer form (into (empty form) 
                                   (doall (map inner form))))
    :else (outer form form)))

(defn is-cache-element? 
  [element]
  "tests is an item is a cache tag"
  (if-let [m-data (meta element)] 
    (::cache m-data)
    false))

(defn make-cache-element
  [id]
  (with-meta (symbol (str "cache-" id)) {::cache true}))

(defn map-from-seq
  [seq]
  (into {} (for [[key value] seq] 
             [key value])))

(defn contains-compressed-elements?
  [value]
  (if (and (coll? value)
           (some is-cache-element? 
                 (flatten (seq value))))
    true
    false))

(defn partition-decompressed-elements
  [cache]
  (let [partition (group-by (fn [[key value]]
                              (if (contains-compressed-elements? value)
                                :compressed
                                :decompressed)) (seq cache))]
    [(map-from-seq (:decompressed partition)) 
     (map-from-seq (:compressed partition))]))

(defn contains-only-keys?
  "looks at t cache values and sees if it only contains keys in keys"
  [cache keys]
  (every? #(if (is-cache-element? %)
             (some #{%} keys)
             true)
          (flatten (seq (last cache)))))

(defn decompress-cache
  [cache]
  (loop [decompressed-cache {}
         cache cache]
    (let [[new-decompressed cache] (partition-decompressed-elements cache)
          decompressed-cache (merge decompressed-cache new-decompressed)]
      (if (empty? cache)
        decompressed-cache
        (let [new-cache
              (into {}
                    (for [[key value] cache]
                      (let [decompressed-value
                            (postwalk-replace decompressed-cache value)
                            value (if (contains-compressed-elements?
                                        decompressed-value)
                                    value
                                    decompressed-value)]
                        [key value])))]
          (recur decompressed-cache new-cache))))))

(defn expand
  "This is the API function to take a cache of elements and expand them"
  [cache]
  ((decompress-cache cache) (make-cache-element 0)))

(def js-counter 1)

(defn check-in-cache
  [element js-values hash-fn]
  (let [hash (hash-fn element)]
    (if (.has js-values hash)
      (.get js-values hash)
      (let [cache-id (make-cache-element js-counter)]
        (set! js-counter (inc js-counter))
        (.set js-values hash cache-id)
        (with-meta element {:cache-id cache-id})))))

(defn side-prewalk
  [inner outer form]
  (side-walk (partial side-prewalk inner outer) 
             outer 
             (inner form)))

(defn cachable?
  "Determines if we can cache an element"
  [element]
  (and
    (not 
      (or (and (vector? element)
               (= 2 (count element)))
          (number? element)
          (keyword? element)
          (string? element)))
    (or  
      (list? element)
      (seq? element)
      (coll? element))))

(defn create-cache-internal
  ([form]
   (create-cache-internal form identity))
  ([form hash-fn]
   (set! js-counter 1)
   (let [compressed-cache #js {}
         js-values (js/Map.)        
         process-element (fn [element]
                           ; don't cache cache elements or [key value] pairs
                           (if (or (identical? element form)
                                   (not (cachable? element)))
                             element
                             (check-in-cache element js-values hash-fn)))
         outer-fn      (fn [org-element element]
                         (if (and (cachable? org-element)
                                  (not (identical? org-element form)))
                           (let [id (:cache-id (meta org-element))]
                             (when (not (nil? id))
                               (aset compressed-cache id element)
                               id))
                           element))
         cache-0       (side-prewalk process-element outer-fn form)]
     ; (print "compressed-cache" compressed-cache)
     ; (print "cache-0" cache-0)
     (aset compressed-cache (make-cache-element 0) cache-0)
     ;(print "The number of unique keys are:" js-counter)
     [(make-cache-element 0)
      (into {}
            (for [key (.keys js/Object compressed-cache)]
              [(symbol key) (aget compressed-cache key)]))
      js-values])))

(defn de-dupe
  "API create an efficient representation for serialization of (immutable persistent) 
   data structures with a lot of structural sharing (uses identical? for comparison"
  [form]
  (let [[message cache values] (create-cache-internal form)]
    cache))

(defn de-dupe-eq
  "API create an efficient representation for serialization of 
   data structures with a lot of shared data (uses = for comparison)"
  [form]
  (let [[message cache values] (create-cache-internal form hash)]
    cache))