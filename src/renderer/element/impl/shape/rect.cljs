(ns renderer.element.impl.shape.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [clojure.string :as str]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.utils.length :as length]))

(derive :rect ::hierarchy/box)
(derive :rect ::hierarchy/shape)

(defmethod hierarchy/properties :rect
  []
  {:icon "rectangle"
   :label "Rectangle"
   :description "The <rect> element is a basic SVG shape that draws rectangles,
                 defined by their position, width, and height. The rectangles
                 may have their corners rounded."
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray
           :stroke-linejoin]})

(defmethod hierarchy/path :rect
  [el]
  (let [{{:keys [x y width height rx ry]} :attrs} el
        [x y width height] (mapv length/unit->px [x y width height])
        rx (length/unit->px (if (and (not rx) ry) ry rx))
        ry (length/unit->px (if (and (not ry) rx) rx ry))
        rx (if (> rx (/ width 2)) (/ width 2) rx)
        ry (if (> ry (/ height 2)) (/ height 2) ry)
        curved? (and (> rx 0) (> ry 0))]
    (cond-> []
      :always (conj "M" (+ x rx) y
                    "H" (- (+ x width) rx))

      curved? (conj "A" rx ry 0 0 1 (+ x width) (+ y ry))

      :always (conj "V" (- (+ y height) ry))

      curved? (conj "A" rx ry 0 0 1 (- (+ x width) rx) (+ y height))

      :always (conj "H" (+ x rx))

      curved? (conj "A" rx ry 0 0 1 x (- (+ y height) ry))

      :always (conj "V" (+ y ry))

      curved? (conj "A" rx ry 0 0 1 (+ x rx) y)

      :always (conj "z")

      :always (->> (str/join " ")))))
