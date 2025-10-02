(ns renderer.utils.vec
  (:require
   [malli.core :as m]))

(m/=> remove-nth [:-> vector? number? vector?])
(defn remove-nth
  "Removes element at index."
  [v index]
  (into (subvec v 0 index)
        (subvec v (inc index))))

(m/=> add [:-> vector? number? any? vector?])
(defn add
  "Adds element to index."
  [v index el]
  (vec (concat
        (subvec v 0 index)
        [el]
        (subvec v index))))

(m/=> move [:-> vector? number? number? vector?])
(defn move
  "Moves element by index."
  [v index-1 index-2]
  (let [el (nth v index-1)]
    (if (= index-1 index-2)
      v
      (-> (remove-nth v index-1)
          (add index-2 el)))))

(m/=> swap [:-> vector? number? number? vector?])
(defn swap
  "Swaps the position of two elements by index."
  [v index-1 index-2]
  (assoc v
         index-2 (v index-1)
         index-1 (v index-2)))
