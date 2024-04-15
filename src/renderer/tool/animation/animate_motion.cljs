(ns renderer.tool.animation.animate-motion
  "https://svgwg.org/specs/animations/#AnimateMotionElement"
  (:require
   [renderer.tool.base :as tool]))

(derive :animateMotion ::tool/animation)

(defmethod tool/properties :animateMotion
  []
  {:description "The SVG <animateMotion> element let define how an element 
                 moves along a motion path."
   :attrs [:keyPoints
           :path
           :rotate]})
