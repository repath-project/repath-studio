(ns renderer.snap.db
  (:require [renderer.utils.math :as math]))

(def options
  [:enum :centers :midpoints :corners :nodes])

(def snap
  [:map {:default {} :closed true}
   [:enabled? {:default true} boolean?]
   [:threshold {:default 15} number?]
   [:nearest-neighbor {:optional true} [:map
                                        [:point math/point]
                                        [:base-point math/point]
                                        [:dist-squared number?]]]
   [:options {:default (set (rest options))} [:set options]]])
