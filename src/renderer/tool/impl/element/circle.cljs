(ns renderer.tool.impl.element.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.document.handlers :as document.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :circle ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :circle
  []
  {:icon "circle-tool"})

(defmethod tool.hierarchy/on-drag :circle
  [db _e]
  (let [offset (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        position (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        [x y] offset
        radius (matrix/distance position offset)
        attrs {:cx x
               :cy y
               :fill (document.handlers/attr db :fill)
               :stroke (document.handlers/attr db :stroke)
               :r radius}]
    (tool.handlers/set-temp db {:type :element :tag :circle :attrs attrs})))

(defmethod tool.hierarchy/snapping-points :circle
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (str (name (:tool db)) " " (if (tool.handlers/temp db) "radius" "center"))})])
