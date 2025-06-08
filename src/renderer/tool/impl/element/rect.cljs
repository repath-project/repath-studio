(ns renderer.tool.impl.element.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.i18n :refer [t]]))

(derive :rect ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :rect
  []
  {:icon "rectangle-tool"
   :label (t [::name "Rectangle"])})

(defmethod tool.hierarchy/help [:rect :create]
  []
  (t [::help [:div "Hold %1 to lock proportions."]] 
     [[:span.shortcut-key "Ctrl"]]))

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
                                        :width (if (:ctrl-key e) (min width height) width)
                                        :height (if (:ctrl-key e) (min width height) height)
                                        :fill (document.handlers/attr db :fill)
                                        :stroke (document.handlers/attr db :stroke)}})))
