(ns renderer.element.utils)

(defn parent-page
  [elements el]
  (loop [parent (:parent el)]
    (when parent
      (let [parent-element (parent elements)]
        (if (= :page (:tag parent-element))
          parent-element
          (recur (:parent parent-element)))))))
