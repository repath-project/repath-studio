(ns repath.studio.helpers)

(defn uid []
  (-> (random-uuid)
      (str)
      (keyword)))

(defn deep-merge [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))

(defn merge-common [f a b]
  (merge-with f
              (select-keys a (keys b))
              (select-keys b (keys a))))

(defn vec-remove
  "Removes element to a collection by index."
  [coll pos]
  (vec (concat
        (subvec coll 0 pos)
        (subvec coll (inc pos)))))

(defn vec-add
  "Adds element to a collection by index."
  [coll pos el]
  (concat (subvec coll 0 pos) [el] (subvec coll pos)))

(defn vec-move
  "Moves element in a collection by index."
  [coll pos1 pos2]
  (let [el (nth coll pos1)]
    (if (= pos1 pos2)
      coll
      (into [] (vec-add (vec-remove coll pos1) pos2 el)))))

(defn vec-swap
  "Swaps the position of two elements in a vector by index."
  [v pos1 pos2]
  (assoc v pos2 (v pos1) pos1 (v pos2)))

(defn parent-page
  [elements element]
  (loop [parent (:parent element)]
    (when parent
      (let [parent-element (parent elements)]
        (if (= :page (:type parent-element))
          parent-element
          (recur (:parent parent-element)))))))
