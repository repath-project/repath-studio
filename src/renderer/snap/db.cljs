(ns renderer.snap.db)

(def options
  [:enum :centers :midpoints :corners :nodes])

(def snap
  [:map {:default {}}
   [:enabled? {:default true} boolean?]
   [:threshold {:default 15} number?]
   [:options {:default (set (rest options))} [:set options]]])
