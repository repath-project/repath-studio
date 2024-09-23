(ns renderer.utils.attribute
  (:require
   [clojure.string :as str]
   [malli.experimental :as mx]
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


(defn str->seq
  [s]
  (-> s str/trim (str/split #"\s*[\s,]\s*")))

(mx/defn points->vec :- vector?
  [points :- string?]
  (into [] (partition-all 2) (str->seq points)))

(def partition-to-px
  (comp
   (map units/unit->px)
   (partition-all 2)))

(mx/defn points->px :- vector?
  [points :- string?]
  (into [] partition-to-px (str->seq points)))
