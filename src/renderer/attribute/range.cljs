(ns renderer.attribute.range
  (:require
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]))

(derive :opacity ::range)

(defmethod hierarchy/form-element [:default ::range]
  [_ k v attrs]
  [v/range-input k v (merge attrs {:min 0
                                   :max 1
                                   :step 0.01})])
