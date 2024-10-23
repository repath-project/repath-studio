(ns renderer.tool.impl.element.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as mat]
   [renderer.document.handlers :as document.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :circle ::hierarchy/element)

(defmethod hierarchy/properties :circle
  []
  {:icon "circle-tool"})

(defmethod hierarchy/drag :circle
  [db]
  (let [offset (:adjusted-pointer-offset db)
        position (:adjusted-pointer-pos db)
        [x y] offset
        radius (mat/distance position offset)
        attrs {:cx x
               :cy y
               :fill (document.h/attr db :fill)
               :stroke (document.h/attr db :stroke)
               :r radius}]
    (h/set-temp db {:type :element :tag :circle :attrs attrs})))
