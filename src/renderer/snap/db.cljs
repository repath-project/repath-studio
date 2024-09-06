(ns renderer.snap.db
  (:require [renderer.utils.math :refer [Vec2D]]))

(def SnapOption
  [:enum :centers :midpoints :corners :nodes])

(def Snap
  [:map {:default {} :closed true}
   [:enabled? {:default true} boolean?]
   [:threshold {:default 15} number?]
   [:nearest-neighbor {:optional true} [:map
                                        [:point Vec2D]
                                        [:base-point Vec2D]
                                        [:dist-squared number?]]]
   [:options {:default (set (rest SnapOption))} [:set SnapOption]]])
