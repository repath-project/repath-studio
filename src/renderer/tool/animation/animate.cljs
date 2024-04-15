(ns renderer.tools.animation.animate
  "https://svgwg.org/specs/animations/#AnimateElement"
  (:require
   [renderer.tools.base :as tools]))

(derive :animate ::tools/animation)

(defmethod tools/properties :animate
  []
  {:description "The SVG <animate> element provides a way to animate an 
                 attribute of an element over time."
   :attrs [:href
           :attributeName
           :begin
           :end
           :dur
           :min
           :max
           :restart
           :repeatCount
           :repeatDur
           :fill
           :calcMode
           :values
           :keyTimes
           :keySplines
           :from
           :to
           :by
           :autoReverse
           :accelerate
           :decelerate
           :additive
           :accumulate
           :id
           :class]})
