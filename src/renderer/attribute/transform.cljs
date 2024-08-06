(ns renderer.attribute.transform
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/transform"
  (:require
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.element.events :as-alias element.e]))

(defmethod hierarchy/description :transform
  []
  "The transform attribute defines a list of transform definitions that are
   applied to an element and the element's children.")
