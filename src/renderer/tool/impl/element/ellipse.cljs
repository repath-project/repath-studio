(ns renderer.tool.impl.element.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [renderer.document.handlers :as document.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.pointer :as pointer]))

(derive :ellipse ::hierarchy/element)

(defmethod hierarchy/properties :ellipse
  []
  {:icon "ellipse-tool"})

(defmethod hierarchy/help [:ellipse :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod hierarchy/drag :ellipse
  [db e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        lock-ratio (pointer/ctrl? e)
        rx (abs (- x offset-x))
        ry (abs (- y offset-y))
        attrs {:cx offset-x
               :cy offset-y
               :fill (document.h/attr db :fill)
               :stroke (document.h/attr db :stroke)
               :rx (if lock-ratio (min rx ry) rx)
               :ry (if lock-ratio (min rx ry) ry)}]
    (h/set-temp db {:type :element
                    :tag :ellipse
                    :attrs attrs})))
