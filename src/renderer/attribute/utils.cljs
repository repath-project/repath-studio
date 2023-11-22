(ns renderer.attribute.utils
  (:require
   [clojure.string :as str]))

(def attrs-order
  [:d
   :points
   :x :y
   :x1 :y1
   :x2 :y2
   :cx :cy
   :dx :dy
   :width :height
   :rx :ry
   :r
   :rotate
   :transform
   :font-family :font-size :font-weight :font-style
   :textLength
   :lengthAdjust
   :viewBox :preserveAspectRatio
   :stroke
   :fill
   :stroke-width :stroke-linecap :stroke-linejoin :stroke-dasharray
   :opacity
   :overflow
   :id :class :tabindex
   :style])

(defn points-to-vec
  [points]
  (vec (as-> points p
         (str/triml p)
         (str/split p #"\s+")
         (partition 2 p))))
