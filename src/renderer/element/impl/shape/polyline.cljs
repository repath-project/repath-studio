(ns renderer.element.impl.shape.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/polyline"
  (:require [renderer.element.hierarchy :as element.hierarchy]
            [renderer.utils.i18n :refer [t]]))

(derive :polyline ::element.hierarchy/polyshape)

(defmethod element.hierarchy/properties :polyline
  []
  {:icon "polyline"
   :label (t [::label "Polyline"])
   :description (t [::description
                    "The <polyline> SVG element is an SVG basic shape that
                     creates straight lines connecting several points. Typically
                     a polyline is used to create open shapes as the last point
                     doesn't have to be connected to the first point."])
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :stroke-linecap
           :stroke-dasharray
           :opacity]})

(defmethod element.hierarchy/path :polyline
  [el]
  (str "M" (-> el :attrs :points)))
