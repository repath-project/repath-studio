(ns renderer.db
  (:require
   [renderer.document.db]
   [renderer.panel.db]
   [renderer.snap.db]
   [renderer.theme.db]
   [renderer.timeline.db]
   [renderer.window.db]))

(def app
  [:map
   [:tool keyword?]
   [:pointer-pos [:tuple double? double?]]
   [:zoom-sensitivity double?]
   [:state keyword?]
   [:grid? boolean?]
   [:rulers? boolean?]
   [:snap renderer.snap.db/snap]
   [:rulers-locked? boolean?]
   [:documents [:map-of :uuid renderer.document.db/document]]
   [:document-tabs [:vector uuid?]]
   [:system-fonts vector?]
   [:debug-info? boolean?]
   [:pen-mode? boolean?]
   [:panel [:map-of :key renderer.panel.db/panel]]
   [:window renderer.window.db/window]
   [:theme [:mode renderer.theme.db/modes]]
   [:timeline renderer.timeline.db/timeline]])

(def default
  {:tool :select
   :pointer-pos [0 0]
   :zoom-sensitivity 0.75
   :state :default
   :documents {}
   :document-tabs []
   :recent []
   :system-fonts []
   :notifications []
   :debug-info? false
   :pen-mode? false
   :rulers-locked? false
   :grid? false
   :rulers? true
   :snap {:enabled? true
          :threshold 100
          :options #{:centers :midpoints :corners :nodes}}
   :lang :en-US
   :repl-mode :cljs
   :theme {:mode :dark}
   :worker {:tasks {}}
   :cmdk {:visible? false}
   :panel {:tree {:visible? true}
           :properties {:visible? true}
           :timeline {:visible? false}
           :xml {:visible? false}
           :repl-history {:visible? false}}
   :window {:maximized? true
            :minimized? false
            :fullscreen? false}
   :timeline {:time 0
              :replay? false
              :grid-snap? false
              :guide-snap? true
              :paused? false
              :speed 1}})
