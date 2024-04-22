(ns renderer.tool.shape.polygon
  "https://www.w3.org/TR/SVG/shapes.html#PolygonElement"
  (:require [renderer.tool.base :as tool]))

(derive :polygon ::tool/polyshape)

(defmethod tool/properties :polygon
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

(defmethod tool/path :polygon
  [{{:keys [points]} :attrs}]
  (str "M" points "z"))
