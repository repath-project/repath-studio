(ns renderer.snap.db)

(def options
  [:enum :centers :midpoints :corners :nodes])

(def snap
  [:map
   [:enabled? boolean?]
   [:threshold double?]
   [:options [:set options]]])
