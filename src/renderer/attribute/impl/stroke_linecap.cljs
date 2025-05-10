(ns renderer.attribute.impl.stroke-linecap
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/stroke-linecap"
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]))

(defmethod attribute.hierarchy/description [:default :stroke-linecap]
  []
  "The stroke-linecap attribute is a presentation attribute defining the shape
   to be used at the end of open subpaths when they are stroked.")

(defmethod attribute.hierarchy/form-element [:default :stroke-linecap]
  [_ k v attrs]
  [attribute.views/select-input k v (merge attrs {:default-value "butt"
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
                                                           :icon "linecap-square"}]})])
