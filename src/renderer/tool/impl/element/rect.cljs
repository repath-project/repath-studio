(ns renderer.tool.impl.element.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.pointer :as pointer]))

(derive :rect ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :rect
  []
  {:icon "rectangle-tool"
   :label "Rectangle"})

(defmethod tool.hierarchy/help [:rect :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod tool.hierarchy/drag :rect
  [db e]
  (let [{:keys [stroke fill]} (get-in db [:documents (:active-document db)])
        [offset-x offset-y] (:adjusted-pointer-offset db)
        [x y] (:adjusted-pointer-pos db)
        lock-ratio (pointer/ctrl? e)
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        attrs {:x (min x offset-x)
               :y (min y offset-y)
               :width (if lock-ratio (min width height) width)
               :height (if lock-ratio (min width height) height)
               :fill fill
               :stroke stroke}]
    (element.h/assoc-temp db {:type :element
                              :tag :rect
                              :attrs attrs})))
