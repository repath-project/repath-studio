(ns renderer.attribute.stroke-linejoin
  (:require
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]))

(defmethod hierarchy/description :stroke-linejoin
  []
  "The stroke-linejoin attribute is a presentation attribute defining the shape 
   to be used at the corners of paths when they are stroked.")

(defmethod hierarchy/form-element :stroke-linejoin
  [k v disabled? initial]
  [v/select-input {:key k
                   :value v
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
