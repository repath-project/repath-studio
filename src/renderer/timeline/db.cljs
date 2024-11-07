(ns renderer.timeline.db)

(def Timeline
  [:map {:closed true}
   [:time {:default 0} number?]
   [:speed {:default 1} number?]
   [:replay? {:default false} boolean?]
   [:grid-snap? {:default false} boolean?]
   [:guide-snap? {:default true} boolean?]
   [:paused? {:default false} boolean?]])
