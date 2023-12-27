(ns renderer.tools.animate-transform
  "https://svgwg.org/specs/animations/#AnimateTransformElement"
  (:require
   [renderer.tools.base :as tools]))

(derive :animateTransform ::tools/animation)

(defmethod tools/properties :animateTransform
  []
  {:description "The animateTransform element animates a transformation 
                 attribute on its target element, thereby allowing animations 
                 to control translation, scaling, rotation, and/or skewing."
   :attrs [:type
           :from
           :to
           :by]})
