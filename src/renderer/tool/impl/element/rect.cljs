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

(defmethod hierarchy/on-drag :rect
  [db e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        width (abs (- x offset-x))
        height (abs (- y offset-y))]
    (h/set-temp db {:type :element
                    :tag :rect
                    :attrs {:x (min x offset-x)
                            :y (min y offset-y)
                            :width (if (pointer/ctrl? e) (min width height) width)
                            :height (if (pointer/ctrl? e) (min width height) height)
                            :fill (document.h/attr db :fill)
                            :stroke (document.h/attr db :stroke)}})))
