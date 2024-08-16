(ns renderer.snap.db)

(def options
  [:enum :centers :midpoints :corners :nodes])

(def snap
  [:map {:default {}}
   [:enabled? [boolean? {:default true}]]
   [:threshold [number? {:default 100}]]
   [:options [:set {:default (set (rest options))} options]]])
