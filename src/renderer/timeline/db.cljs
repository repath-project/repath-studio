(ns renderer.timeline.db)

(def timeline
  [:map
   [:time number?]
   [:replay? boolean?]
   [:grid-snap? boolean?]
   [:guide-snap? boolean?]
   [:paused? boolean?]])
