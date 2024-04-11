(ns renderer.snap.db)

(def snap
  [:map
   [:enabled? boolean?]
   [:threshold double?]
   [:options [:set keyword?]]])
