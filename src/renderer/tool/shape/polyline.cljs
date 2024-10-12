(ns renderer.tool.shape.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement"
  (:require [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :polyline ::tool.hierarchy/polyshape)

(defmethod tool.hierarchy/properties :polyline
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

(defmethod tool.hierarchy/path :polyline
  [el]
  (str "M" (-> el :attrs :points)))
