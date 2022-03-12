(ns repath.studio.tools.animate-motion
  (:require [repath.studio.tools.base :as tools]))

(derive :animateMotion ::tools/animation)

(defmethod tools/properties :animateMotion [] {:description "The SVG <animateMotion> element let define how an element moves along a motion path."})


