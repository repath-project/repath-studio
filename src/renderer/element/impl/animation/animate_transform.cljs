(ns renderer.element.impl.animation.animate-transform
  "https://svgwg.org/specs/animations/#AnimateTransformElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/animateTransform"
  (:require
   [renderer.element.hierarchy :as hierarchy]))

(derive :animateTransform ::hierarchy/animation)

(defmethod hierarchy/properties :animateTransform
  []
  {:icon "animation"
   :description "The animateTransform element animates a transformation
                 attribute on its target element, thereby allowing animations
                 to control translation, scaling, rotation, and/or skewing."
   :attrs [:type
           :from
           :to
           :by]})
