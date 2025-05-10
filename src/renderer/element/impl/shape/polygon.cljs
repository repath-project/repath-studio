(ns renderer.element.impl.shape.polygon
  "https://www.w3.org/TR/SVG/shapes.html#PolygonElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/polygon"
  (:require [renderer.element.hierarchy :as element.hierarchy]))

(derive :polygon ::element.hierarchy/polyshape)

(defmethod element.hierarchy/properties :polygon
  []
  {:icon "polygon"
   :description "The <polyline> SVG element is an SVG basic shape that creates
                 straight lines connecting several points."
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :stroke-linecap
           :stroke-dasharray
           :opacity]})

(defmethod element.hierarchy/path :polygon
  [el]
  (str "M" (-> el :attrs :points) "z"))
