(ns renderer.tool.animation.animate-transform
  "https://svgwg.org/specs/animations/#AnimateTransformElement"
  (:require
   [renderer.tool.base :as tool]))

(derive :animateTransform ::tool/animation)

(defmethod tool/properties :animateTransform
  []
  {:description "The animateTransform element animates a transformation 
                 attribute on its target element, thereby allowing animations 
                 to control translation, scaling, rotation, and/or skewing."
   :attrs [:type
           :from
           :to
           :by]})
