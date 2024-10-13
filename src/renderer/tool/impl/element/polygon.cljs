(ns renderer.tool.impl.element.polygon
  "https://www.w3.org/TR/SVG/shapes.html#PolygonElement"
  (:require [renderer.tool.hierarchy :as hierarchy]))

(derive :polygon ::hierarchy/polyshape)

(defmethod hierarchy/properties :polygon
  []
  {:icon "polygon-tool"})
