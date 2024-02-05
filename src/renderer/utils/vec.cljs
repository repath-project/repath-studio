(ns renderer.utils.vec)

(defn remove-by-index
  "Removes element by index."
  [coll index]
  (vec (concat
        (subvec coll 0 index)
        (subvec coll (inc index)))))

(defn add
  "Adds element by index."
  [coll index el]
  (vec (concat
        (subvec coll 0 index)
        [el]
        (subvec coll index))))

(defn move
  "Moves element by index."
  [coll index-1 index-2]
  (let [el (nth coll index-1)]
    (if (= index-1 index-2)
      coll
      (-> (remove-by-index coll index-1)
          (add index-2 el)
          vec))))

(defn swap
  "Swaps the position of two elements by index."
  [coll index-1 index-2]
  (assoc coll
         index-2 (coll index-1)
         index-1 (coll index-2)))
