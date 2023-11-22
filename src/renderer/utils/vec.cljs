(ns renderer.utils.vec)

(defn remove-by-index
  "Removes element by index."
  [coll pos]
  (vec (concat
        (subvec coll 0 pos)
        (subvec coll (inc pos)))))

(defn add
  "Adds element by index."
  [coll pos el]
  (vec (concat
        (subvec coll 0 pos)
        [el]
        (subvec coll pos))))

#_(defn move
    "Moves element by index."
    [coll pos1 pos2]
    (let [el (nth coll pos1)]
      (if (= pos1 pos2)
        coll
        (into [] (add (remove-by-index coll pos1) pos2 el)))))

(defn swap
  "Swaps the position of two elements by index."
  [coll pos1 pos2]
  (assoc coll
         pos2 (coll pos1)
         pos1 (coll pos2)))
