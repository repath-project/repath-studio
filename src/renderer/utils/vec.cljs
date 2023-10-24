(ns renderer.utils.vec)

(defn remove-by-index
  "Removes element to a collection by index."
  [coll pos]
  (vec (concat
        (subvec coll 0 pos)
        (subvec coll (inc pos)))))

#_(defn add
    "Adds element to a collection by index."
    [coll pos el]
    (concat (subvec coll 0 pos) [el] (subvec coll pos)))

#_(defn move
    "Moves element in a collection by index."
    [coll pos1 pos2]
    (let [el (nth coll pos1)]
      (if (= pos1 pos2)
        coll
        (into [] (add (remove-by-index coll pos1) pos2 el)))))

(defn swap
  "Swaps the position of two elements in a vector by index."
  [v pos1 pos2]
  (assoc v pos2 (v pos1) pos1 (v pos2)))