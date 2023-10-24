(ns renderer.tools.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require [renderer.elements.handlers :as elements]
            [renderer.tools.base :as tools]))

(derive :rect ::tools/box)
(derive :rect ::tools/shape)

(defmethod tools/properties :rect
  []
  {:icon "rectangle"
   :description "The <rect> element is a basic SVG shape that draws rectangles, 
                 defined by their position, width, and height. The rectangles 
                 may have their corners rounded."
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray
           :stroke-linejoin]})

(defmethod tools/drag :rect
  [{:keys [adjusted-mouse-offset active-document adjusted-mouse-pos] :as db}
   event]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        lock-ratio? (contains? (:modifiers event) :ctrl)
        width (abs (- pos-x offset-x))
        height (abs (- pos-y offset-y))
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :width (if lock-ratio? (min width height) width)
               :height (if lock-ratio? (min width height) height)
               :fill fill
               :stroke stroke}]
    (elements/set-temp db {:type :element :tag :rect :attrs attrs})))