(ns renderer.utils.vec
  (:require
   [malli.core :as m]))

(m/=> remove-nth [:-> vector? number? vector?])
(defn remove-nth
  "Removes element at index."
  [v i]
  (into (subvec v 0 i)
        (subvec v (inc i))))

(m/=> add [:-> vector? number? any? vector?])
(defn add
  "Adds element to index."
  [v i el]
  (vec (concat
        (subvec v 0 i)
        [el]
        (subvec v i))))

(m/=> move [:-> vector? number? number? vector?])
(defn move
  "Moves element by index."
  [v i-1 i-2]
  (let [el (nth v i-1)]
    (if (= i-1 i-2)
      v
      (-> (remove-nth v i-1)
          (add i-2 el)))))

(m/=> swap [:-> vector? number? number? vector?])
(defn swap
  "Swaps the position of two elements by index."
  [v i-1 i-2]
  (assoc v
         i-2 (v i-1)
         i-1 (v i-2)))
