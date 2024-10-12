(ns renderer.tool.impl.element.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.pointer :as pointer]))

(derive :ellipse ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :ellipse
  []
  {:icon "ellipse-tool"})

(defmethod tool.hierarchy/help [:ellipse :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod tool.hierarchy/drag :ellipse
  [db e]
  (let [{:keys [stroke fill]} (get-in db [:documents (:active-document db)])
        [offset-x offset-y] (:adjusted-pointer-offset db)
        [x y] (:adjusted-pointer-pos db)
        lock-ratio (pointer/ctrl? e)
        rx (abs (- x offset-x))
        ry (abs (- y offset-y))
        attrs {:cx offset-x
               :cy offset-y
               :fill fill
               :stroke stroke
               :rx (if lock-ratio (min rx ry) rx)
               :ry (if lock-ratio (min rx ry) ry)}]
    (element.h/assoc-temp db {:type :element
                              :tag :ellipse
                              :attrs attrs})))
