(ns renderer.attribute.stroke-linecap
  (:require
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as views]))

(defmethod hierarchy/description :stroke-linecap
  []
  "The stroke-linecap attribute is a presentation attribute defining the shape 
   to be used at the end of open subpaths when they are stroked.")

(defmethod hierarchy/form-element :stroke-linecap
  [key value disabled? initial]
  [views/select-input {:key key
                       :value value
                       :disabled? disabled?
                       :initial initial
                       :default-value "butt"
                       :items [{:key :butt
                                :value "butt"
                                :label "Butt"
                                :icon "linecap-butt"}
                               {:key :round
                                :value "round"
                                :label "Round"
                                :icon "linecap-round"}
                               {:key :square
                                :value "square"
                                :label "Square"
                                :icon "linecap-square"}]}])
