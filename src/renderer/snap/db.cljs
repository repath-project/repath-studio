(ns renderer.snap.db
  (:require [renderer.utils.math :refer [Vec2D]]))

(def snap-options
  [:centers :midpoints :corners :nodes])

(def SnapOption
  (into [:enum] snap-options))

(def Snap
  [:map {:default {} :closed true}
   [:active {:default true} boolean?]
   [:threshold {:default 15} number?]
   [:nearest-neighbor {:optional true} [:map
                                        [:point Vec2D]
                                        [:base-point Vec2D]
                                        [:dist-squared number?]]]
   [:options {:default (set snap-options)} [:set SnapOption]]])
