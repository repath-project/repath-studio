(ns renderer.tools.animation.animate-motion
  "https://svgwg.org/specs/animations/#AnimateMotionElement"
  (:require
   [renderer.tools.base :as tools]))

(derive :animateMotion ::tools/animation)

(defmethod tools/properties :animateMotion
  []
  {:description "The SVG <animateMotion> element let define how an element 
                 moves along a motion path."
   :attrs [:keyPoints
           :path
           :rotate]})
