(ns renderer.utils.spec
  "Use BCD to get compatibility data for properties and more.
   https://github.com/mdn/browser-compat-data"
  (:require ["@mdn/browser-compat-data" :as bcd]))

(def svg
  (js->clj (.-svg bcd) :keywordize-keys true))

(def core-attrs
  #{:id :class :style})

(def presentation-attrs
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

(defn compat-data
  "Returns conmpatibility data for tags or attributes."
  ([tag]
   (-> svg :elements tag :__compat))
  ([tag attr]
   (-> svg :elements tag attr :__compat)))
