(ns renderer.tool.animation.animate-transform
  "https://svgwg.org/specs/animations/#AnimateTransformElement"
  (:require
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :animateTransform ::tool.hierarchy/animation)

(defmethod tool.hierarchy/properties :animateTransform
  []
  {:description "The animateTransform element animates a transformation
                 attribute on its target element, thereby allowing animations
                 to control translation, scaling, rotation, and/or skewing."
   :attrs [:type
           :from
           :to
           :by]})
