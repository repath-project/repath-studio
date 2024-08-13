(ns renderer.utils.attribute
  (:require
   [clojure.string :as str]
   [renderer.utils.units :as units]))

(def core
  #{:id :class :style})

(def presentation
  #{:text-anchor :text-rendering :font-style :mask :image-rendering
    :stroke-dasharray :fill-rule :font-stretch :text-overflow :vector-effect
    :stroke :stop-color :clip :glyph-orientation-horizontal :solid-opacity
    :transform :color :white-space :font-size :kerning :font-variant
    :writing-mode :font-weight :overflow :clip-rule :stroke-opacity :fill
    :color-profile :stroke-linejoin :shape-rendering :cursor :stroke-dashoffset
    :word-spacing :clip-path :stroke-linecap :flood-opacity :lighting-color
    :alignment-baseline :dominant-baseline :marker-start :filter :stroke-width
    :opacity :baseline-shift :color-interpolation-filters :transform-origin
    :text-decoration :display :stroke-miterlimit :letter-spacing :flood-color
    :unicode-bidi :marker-mid :pointer-events :font-size-adjust
    :glyph-orientation-vertical :color-interpolation :visibility
    :enable-background :direction :fill-opacity :solid-color :font-family
    :marker-end :paint-order})

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
    (str/split p #"\s*[\s,]\s*")
    (partition 2 p)
    (vec p))) ; OPTIMIZE

(defn points->px
  [points]
  (as-> points p
    (str/triml p)
    (str/split p #"\s*[\s,]\s*")
    (map units/unit->px p)
    (partition 2 p)
    (vec p)))
