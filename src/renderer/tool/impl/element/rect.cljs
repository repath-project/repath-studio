(ns renderer.tool.impl.element.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [renderer.document.handlers :as document.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.pointer :as pointer]))

(derive :rect ::hierarchy/element)

(defmethod hierarchy/properties :rect
  []
  {:icon "rectangle-tool"
   :label "Rectangle"})

(defmethod hierarchy/help [:rect :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod hierarchy/drag :rect
  [db e]
  (let [[offset-x offset-y] (:adjusted-pointer-offset db)
        [x y] (:adjusted-pointer-pos db)
        lock-ratio (pointer/ctrl? e)
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        attrs {:x (min x offset-x)
               :y (min y offset-y)
               :width (if lock-ratio (min width height) width)
               :height (if lock-ratio (min width height) height)
               :fill (document.h/attr db :fill)
               :stroke (document.h/attr db :stroke)}]
    (h/set-temp db {:type :element
                    :tag :rect
                    :attrs attrs})))
