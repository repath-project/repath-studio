(ns renderer.attribute.range
  (:require
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as views]))

(derive :opacity ::range)

(defmethod hierarchy/form-element ::range
  [key value disabled? initial]
  [views/range-input key value {:disabled disabled?
                                :min 0
                                :max 1
                                :step 0.01} initial])
