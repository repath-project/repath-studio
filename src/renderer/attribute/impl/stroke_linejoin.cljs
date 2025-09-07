(ns renderer.attribute.impl.stroke-linejoin
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/stroke-linejoin"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.utils.i18n :refer [t]]))

(defmethod attribute.hierarchy/description [:default :stroke-linejoin]
  []
  (t [::description
      "The stroke-linejoin attribute is a presentation attribute defining the shape
       to be used at the corners of paths when they are stroked."]))

(defmethod attribute.hierarchy/form-element [:default :stroke-linejoin]
  [_ k v attrs]
  [attribute.views/select-input k v
   (merge attrs {:default-value "miter"
                 :items [{:key :bevel
                          :value "bevel"
                          :label (t [::bevel "Bevel"])}
                         {:key :miter
                          :value "miter"
                          :label (t [::miter "Miter"])}
                         {:key :round
                          :value "round"
                          :label (t [::round "Round"])}]})])
