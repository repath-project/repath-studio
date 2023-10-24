(ns renderer.attribute.stroke-linejoin
  (:require
   [renderer.attribute.views :as views]
   [renderer.attribute.hierarchy :as hierarchy]))

(defmethod hierarchy/description :stroke-linejoin
  []
  "The stroke-linejoin attribute is a presentation attribute defining the shape 
   to be used at the corners of paths when they are stroked.")

(defmethod hierarchy/form-element :stroke-linejoin
  [key value disabled? initial]
  [views/select-input {:key key
                       :value value
                       :disabled? disabled?
                       :initial initial
                       :default-value "miter"
                       :items [{:key :bevel
                                :value "bevel"
                                :label "Bevel"}
                               {:key :miter
                                :value "miter"
                                :label "Miter"}
                               {:key :round
                                :value "round"
                                :label "Round"}]}])
