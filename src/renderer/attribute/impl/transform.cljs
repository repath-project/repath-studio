(ns renderer.attribute.impl.transform
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/transform"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.utils.i18n :refer [t]]))

(defmethod attribute.hierarchy/description [:default :transform]
  []
  (t ["The transform attribute defines a list of transform definitions that are
       applied to an element and the element's children."]))
