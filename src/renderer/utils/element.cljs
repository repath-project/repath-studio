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
        (if (container? parent-element)
          parent-element
          (recur (:parent parent-element)))))))

(defn adjusted-bounds
  [element elements]
  (when-let [bounds (tools/bounds element elements)]
    (if-let [container (parent-container elements element)]
      (let [[offset-x offset-y _ _] (tools/bounds container elements)
            [x1 y1 x2 y2] bounds]
        [(+ x1 offset-x) (+ y1 offset-y)
         (+ x2 offset-x) (+ y2 offset-y)])
      bounds)))

(defn bounds
  [elements bound-elements]
  (let [bounds (->> bound-elements
                    (map #(adjusted-bounds % elements))
                    (remove nil?))]
    (when (seq bounds)
      (apply bounds/union bounds))))
