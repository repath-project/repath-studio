(ns renderer.element.impl.animation.animate-motion
  "https://svgwg.org/specs/animations/#AnimateMotionElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/animateMotion"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]))

(derive :animateMotion ::element.hierarchy/animation)

(defmethod element.hierarchy/properties :animateMotion
  []
  {:icon "animation"
   :description "The SVG <animateMotion> element let define how an element
                 moves along a motion path."
   :attrs [:keyPoints
           :path
           :rotate]})
