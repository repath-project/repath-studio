(ns renderer.utils.vec)

(defn remove-nth
  "Removes element at index."
  [v i]
  (into
   (subvec v 0 i)
   (subvec v (inc i))))

(defn add
  "Adds element to index."
  [v index el]
  (vec (concat
        (subvec v 0 index)
        [el]
        (subvec v index))))

(defn move
  "Moves element by index."
  [v i-1 i-2]
  (let [el (nth v i-1)]
    (if (= i-1 i-2)
      v
      (-> (remove-nth v i-1)
          (add i-2 el)
          vec))))

(defn swap
  "Swaps the position of two elements by index."
  [v i-1 i-2]
  (assoc v
         i-2 (v i-1)
         i-1 (v i-2)))
