(ns renderer.tool.impl.element.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.i18n :refer [t]]))

(derive :svg ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :svg
  []
  {:icon "svg"})

(defmethod tool.hierarchy/help [:svg :create]
  []
  (t [::help [:div "Hold %1 to lock proportions."]]
     [[:span.shortcut-key "Ctrl"]]))

(defmethod tool.hierarchy/on-drag :svg
  [db e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        lock-ratio (:ctrl-key e)
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        attrs {:x (min x offset-x)
               :y (min y offset-y)
               :width (if lock-ratio (min width height) width)
               :height (if lock-ratio (min width height) height)}]
    (tool.handlers/set-temp db {:tag :svg
                                :type :element
                                :attrs attrs})))
