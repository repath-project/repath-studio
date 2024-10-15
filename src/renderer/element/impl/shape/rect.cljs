(ns renderer.element.impl.shape.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [clojure.string :as str]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.utils.units :as units]))

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
        [x y width height] (mapv units/unit->px [x y width height])
        rx (units/unit->px (if (and (not rx) ry) ry rx))
        ry (units/unit->px (if (and (not ry) rx) rx ry))
        rx (if (> rx (/ width 2)) (/ width 2) rx)
        ry (if (> ry (/ height 2)) (/ height 2) ry)
        curved? (and (> rx 0) (> ry 0))]
    (->> ["M" (+ x rx) y
          "H" (- (+ x width) rx)
          (when curved? (str/join " " ["A" rx ry 0 0 1 (+ x width) (+ y ry)]))
          "V" (- (+ y height) ry)
          (when curved? (str/join " " ["A" rx ry 0 0 1 (- (+ x width) rx) (+ y height)]))
          "H" (+ x rx)
          (when curved? (str/join " " ["A" rx ry 0 0 1 x (- (+ y height) ry)]))
          "V" (+ y ry)
          (when curved? (str/join " " ["A" rx ry 0 0 1 (+ x rx) y]))
          "z"]
         (remove nil?)
         (str/join " "))))
