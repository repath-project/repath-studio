(ns renderer.tools.shape.polygon
  "https://www.w3.org/TR/SVG/shapes.html#PolygonElement"
  (:require [renderer.tools.base :as tools]))

(derive :polygon ::tools/polyshape)

(defmethod tools/properties :polygon
  []
  {:icon "polygon-alt"
   :description "The <polyline> SVG element is an SVG basic shape that creates 
                 straight lines connecting several points."
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :stroke-linecap
           :stroke-dasharray
           :opacity]})
