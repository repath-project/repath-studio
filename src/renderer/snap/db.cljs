(ns renderer.snap.db
  (:require [renderer.utils.math :refer [Vec2]]))

(def snap-options
  [:centers :midpoints :corners :nodes #_:grid])

(def SnapOption
  (into [:enum] snap-options))

(def SnapOptions
  [:set SnapOption])

(def NearestNeighbor
  [:map {:closed true}
   [:point Vec2]
   [:base-point Vec2]
   [:dist-squared number?]])

(def Snap
  [:map {:closed true}
   [:active {:default true} boolean?]
   [:threshold {:default 15} number?]
   [:options {:default (set snap-options)} SnapOptions]])
