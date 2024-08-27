(ns renderer.utils.vec
  (:require
   [malli.experimental :as mx]))

(mx/defn remove-nth :- vector?
  "Removes element at index."
  [v :- vector? i :- number?]
  (into
   (subvec v 0 i)
   (subvec v (inc i))))

(mx/defn add :- vector?
  "Adds element to index."
  [v :- vector? index el]
  (vec (concat
        (subvec v 0 index)
        [el]
        (subvec v index))))

(mx/defn move :- vector?
  "Moves element by index."
  [v :- vector? i-1 :- number? i-2 :- number?]
  (let [el (nth v i-1)]
    (if (= i-1 i-2)
      v
      (-> (remove-nth v i-1)
          (add i-2 el)
          (vec)))))

(mx/defn swap :- vector?
  "Swaps the position of two elements by index."
  [v :- vector? i-1 :- number? i-2 :- number?]
  (assoc v
         i-2 (v i-1)
         i-1 (v i-2)))
