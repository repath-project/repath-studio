(ns renderer.tool.impl.element.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.pointer :as utils.pointer]))

(derive :rect ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :rect
  []
  {:icon "rectangle-tool"
   :label "Rectangle"})

(defmethod tool.hierarchy/help [:rect :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod tool.hierarchy/on-drag :rect
  [db e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        width (abs (- x offset-x))
        height (abs (- y offset-y))]
    (tool.handlers/set-temp db {:type :element
                                :tag :rect
                                :attrs {:x (min x offset-x)
                                        :y (min y offset-y)
                                        :width (if (utils.pointer/ctrl? e) (min width height) width)
                                        :height (if (utils.pointer/ctrl? e) (min width height) height)
                                        :fill (document.handlers/attr db :fill)
                                        :stroke (document.handlers/attr db :stroke)}})))
