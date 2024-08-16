(ns renderer.timeline.db)

(def timeline
  [:map {:default {}}
   [:time [number? {:default 0}]]
   [:speed [number? {:default 1}]]
   [:replay? [boolean? {:default false}]]
   [:grid-snap? [boolean? {:default false}]]
   [:guide-snap? [boolean? {:default true}]]
   [:paused? [boolean? {:default false}]]])
