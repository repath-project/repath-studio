(ns renderer.tool.impl.element.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement"
  (:require [renderer.tool.hierarchy :as hierarchy]))

(derive :polyline ::hierarchy/polyshape)

(defmethod hierarchy/properties :polyline
  []
  {:icon "polyline"})
