(ns renderer.tool.impl.element.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement"
  (:require [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :polyline ::tool.hierarchy/polyshape)

(defmethod tool.hierarchy/properties :polyline
  []
  {:icon "polyline"})
