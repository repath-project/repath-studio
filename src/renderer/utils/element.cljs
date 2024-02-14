(ns renderer.utils.element
  (:require
   [renderer.tools.base :as tools]
   [renderer.utils.bounds :as bounds]))

(defn root?
  [el]
  (= :canvas (:tag el)))

(defn svg?
  [el]
  (= :svg (:tag el)))

(defn container?
  [el]
  #_(isa? (:tag el) ::tools/container)
  (or (svg? el) (root? el)))

(defn parent-container
  [elements el]
  (loop [parent (:parent el)]
    (when parent
      (let [parent-element (parent elements)]
        (if (or (= :canvas (:tag parent-element))
                (container? parent-element))
          parent-element
          (recur (:parent parent-element)))))))

(defn adjusted-bounds
  [element elements]
  (if-let [container (parent-container elements element)]
    (let [[offset-x offset-y _ _] (tools/bounds container elements)
          [x1 y1 x2 y2] (tools/bounds element elements)]
      [(+ x1 offset-x) (+ y1 offset-y)
       (+ x2 offset-x) (+ y2 offset-y)])
    (let [b (tools/bounds element elements)]
      (when (seq b) b))))

(defn bounds
  [elements bound-elements]
  (when (seq bound-elements)
    (apply bounds/union (map #(adjusted-bounds % elements) bound-elements))))
