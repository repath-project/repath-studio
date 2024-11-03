(ns renderer.snap.db
  (:require [renderer.utils.math :refer [Vec2D]]))

(def snap-options
  [:centers :midpoints :corners :nodes #_:grid])

(def SnapOption
  (into [:enum] snap-options))

(def SnapOptions
  [:set SnapOption])

(def NearestNeighbor
  [:map
   [:point Vec2D]
   [:base-point Vec2D]
   [:dist-squared number?]])

(def Snap
  [:map {:default {} :closed true}
   [:active {:default true} boolean?]
   [:threshold {:default 15} number?]
   [:options {:default (set snap-options)} SnapOptions]])
