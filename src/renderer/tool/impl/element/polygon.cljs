(ns renderer.tool.impl.element.polygon
  "https://www.w3.org/TR/SVG/shapes.html#PolygonElement"
  (:require [renderer.tool.hierarchy :as tool.hierarchy]
            [renderer.utils.i18n :refer [t]]))

(derive :polygon ::tool.hierarchy/polyshape)

(defmethod tool.hierarchy/properties :polygon
  []
  {:icon "polygon-tool"
   :label (t [::label "Polygon"])})
