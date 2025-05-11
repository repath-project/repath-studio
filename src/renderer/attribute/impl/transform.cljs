(ns renderer.attribute.impl.transform
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/transform"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]))

(defmethod attribute.hierarchy/description [:default :transform]
  []
  "The transform attribute defines a list of transform definitions that are
   applied to an element and the element's children.")
