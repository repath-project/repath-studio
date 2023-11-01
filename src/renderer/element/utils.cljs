(ns renderer.element.utils)

(defn parent-page
  [elements element]
  (loop [parent (:parent element)]
    (when parent
      (let [parent-element (parent elements)]
        (if (= :page (:tag parent-element))
          parent-element
          (recur (:parent parent-element)))))))