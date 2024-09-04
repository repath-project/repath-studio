(ns renderer.tool.animation.animate-motion
  "https://svgwg.org/specs/animations/#AnimateMotionElement"
  (:require
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :animateMotion ::tool.hierarchy/animation)

(defmethod tool.hierarchy/properties :animateMotion
  []
  {:description "The SVG <animateMotion> element let define how an element
                 moves along a motion path."
   :attrs [:keyPoints
           :path
           :rotate]})
