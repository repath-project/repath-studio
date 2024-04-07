(ns renderer.attribute.utils
  (:require
   [clojure.string :as str]
   [renderer.utils.units :as units]))

(def order
  [:href
   :d
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
   :crossorigin
   :decoding
   :opacity
   :overflow
   :id :class :tabindex
   :style])

(defn points->vec
  [points]
  (as-> points p
    (str/triml p)
    (str/split p #"\s+")
    (partition 2 p)
    (vec p))) ; OPTIMIZE

(defn points->px
  [points]
  (as-> points p
    (str/triml p)
    (str/split p #"\s+")
    (map units/unit->px p)
    (partition 2 p)
    (vec p)))
