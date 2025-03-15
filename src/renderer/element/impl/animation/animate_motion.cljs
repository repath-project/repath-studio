(ns renderer.element.impl.animation.animate-motion
  "https://svgwg.org/specs/animations/#AnimateMotionElement"
  (:require
   [renderer.element.hierarchy :as hierarchy]))

(derive :animateMotion ::hierarchy/animation)

(defmethod hierarchy/properties :animateMotion
  []
  {:icon "animation"
   :description "The SVG <animateMotion> element let define how an element
                 moves along a motion path."
   :attrs [:keyPoints
           :path
           :rotate]})
