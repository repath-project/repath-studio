(ns renderer.tool.impl.element.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :ellipse ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :ellipse
  []
  {:icon "ellipse-tool"})

(defmethod tool.hierarchy/help [:ellipse :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod tool.hierarchy/on-drag :ellipse
  [db e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        lock-ratio (:ctrl-key e)
        rx (abs (- x offset-x))
        ry (abs (- y offset-y))
        attrs {:cx offset-x
               :cy offset-y
               :fill (document.handlers/attr db :fill)
               :stroke (document.handlers/attr db :stroke)
               :rx (if lock-ratio (min rx ry) rx)
               :ry (if lock-ratio (min rx ry) ry)}]
    (tool.handlers/set-temp db {:type :element
                                :tag :ellipse
                                :attrs attrs})))
