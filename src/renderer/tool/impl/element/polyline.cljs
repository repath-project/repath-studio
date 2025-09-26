(ns renderer.tool.impl.element.polyline
  "https://www.w3.org/TR/SVG/shapes.html#PolylineElement"
  (:require [renderer.tool.hierarchy :as tool.hierarchy]
            [renderer.utils.i18n :refer [t]]))

(derive :polyline ::tool.hierarchy/poly)

(defmethod tool.hierarchy/properties :polyline
  []
  {:icon "polyline"
   :label (t [::label "Polyline"])})
