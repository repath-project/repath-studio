(ns renderer.tool.impl.element.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.pointer :as pointer]))

(derive :svg ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :svg
  []
  {:icon "svg"})

(defmethod tool.hierarchy/help [:svg :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod tool.hierarchy/drag :svg
  [db e]
  (let [[offset-x offset-y] (:adjusted-pointer-offset db)
        [x y] (:adjusted-pointer-pos db)
        lock-ratio (pointer/ctrl? e)
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        attrs {:x (min x offset-x)
               :y (min y offset-y)
               :width (if lock-ratio (min width height) width)
               :height (if lock-ratio (min width height) height)}]
    (element.h/assoc-temp db {:tag :svg
                              :type :element
                              :attrs attrs})))
