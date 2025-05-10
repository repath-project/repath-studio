(ns renderer.element.impl.shape.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/polyline"
  (:require [renderer.element.hierarchy :as element.hierarchy]))

(derive :polyline ::element.hierarchy/polyshape)

(defmethod element.hierarchy/properties :polyline
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

(defmethod element.hierarchy/path :polyline
  [el]
  (str "M" (-> el :attrs :points)))
