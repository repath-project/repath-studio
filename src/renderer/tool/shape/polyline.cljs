(ns renderer.tools.shape.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement"
  (:require [renderer.tools.base :as tools]))

(derive :polyline ::tools/polyshape)

(defmethod tools/properties :polyline
  []
  {:icon "polyline"
   :description "The <polyline> SVG element is an SVG basic shape that creates 
                 straight lines connecting several points."
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linecap
           :stroke-dasharray
           :stroke-linejoin
           :opacity]})
