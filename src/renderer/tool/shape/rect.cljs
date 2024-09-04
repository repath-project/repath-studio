(ns renderer.tool.shape.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [clojure.string :as str]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :rect ::tool.hierarchy/box)
(derive :rect ::tool.hierarchy/shape)

(defmethod tool.hierarchy/properties :rect
  []
  {:icon "rectangle-alt"
   :description "The <rect> element is a basic SVG shape that draws rectangles,
                 defined by their position, width, and height. The rectangles
                 may have their corners rounded."
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray
           :stroke-linejoin]})

(defmethod tool.hierarchy/drag :rect
  [{:keys [adjusted-pointer-offset active-document adjusted-pointer-pos] :as db} e]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        lock-ratio? (pointer/ctrl? e)
        width (abs (- pos-x offset-x))
        height (abs (- pos-y offset-y))
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :width (if lock-ratio? (min width height) width)
               :height (if lock-ratio? (min width height) height)
               :fill fill
               :stroke stroke}]
    (element.h/set-temp db {:type :element
                            :tag :rect
                            :attrs attrs})))

(defmethod tool.hierarchy/path :rect
  [{{:keys [x y width height rx ry]} :attrs}]
  (let [[x y width height] (mapv units/unit->px [x y width height])
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
