(ns renderer.attribute.range
  (:require
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as views]))

(derive :opacity ::range)

(defmethod hierarchy/form-element ::range
  [k v disabled? initial]
  [views/range-input k v {:disabled disabled?
                          :min 0
                          :max 1
                          :step 0.01} initial])
