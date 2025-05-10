(ns renderer.element.impl.shape.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/rect"
  (:require
   [clojure.string :as string]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.utils.length :as utils.length]))

(derive :rect ::element.hierarchy/box)
(derive :rect ::element.hierarchy/shape)

(defmethod element.hierarchy/properties :rect
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

(defmethod element.hierarchy/path :rect
  [el]
  (let [{{:keys [x y width height rx ry]} :attrs} el
        [x y width height] (mapv utils.length/unit->px [x y width height])
        rx (utils.length/unit->px (if (and (not rx) ry) ry rx))
        ry (utils.length/unit->px (if (and (not ry) rx) rx ry))
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
      :always (->> (string/join " ")))))
