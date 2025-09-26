(ns renderer.element.impl.shape.polygon
  "https://www.w3.org/TR/SVG/shapes.html#PolygonElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/polygon"
  (:require [renderer.element.hierarchy :as element.hierarchy]
            [renderer.utils.i18n :refer [t]]))

(derive :polygon ::element.hierarchy/poly)

(defmethod element.hierarchy/properties :polygon
  []
  {:icon "polygon"
   :label (t [::label "Polygon"])
   :description (t [::description
                    "The <polygon> SVG element defines a closed shape consisting
                     of a set of connected straight line segments. The last
                     point is connected to the first point."])
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
