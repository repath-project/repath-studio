(ns renderer.snap.db
  (:require [renderer.utils.math :refer [vec2d]]))

(def options
  [:enum :centers :midpoints :corners :nodes])

(def snap
  [:map {:default {} :closed true}
   [:enabled? {:default true} boolean?]
   [:threshold {:default 15} number?]
   [:nearest-neighbor {:optional true} [:map
                                        [:point vec2d]
                                        [:base-point vec2d]
                                        [:dist-squared number?]]]
   [:options {:default (set (rest options))} [:set options]]])
