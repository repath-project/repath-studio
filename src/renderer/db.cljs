(ns renderer.db
  (:require
   [renderer.dialog.db]
   [renderer.document.db]
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
   [:grid-visible? boolean?]
   [:rulers-visible? boolean?]
   [:snap renderer.snap.db/snap]
   [:rulers-locked? boolean?]
   [:dialogs [:vector renderer.dialog.db/dialog]]
   [:documents [:map-of keyword? map?]]
   [:document-tabs [:vector keyword?]]
   [:recent [:vector string?]]
   [:system-fonts vector?]
   [:notifications vector?]
   [:debug-info? boolean?]
   [:pen-mode? boolean?]
   [:backdrop? boolean?]
   [:lang keyword?]
   [:repl-mode keyword?]
   [:worker [:map [:tasks set?]]]
   [:panel [:map-of keyword? [:map [:visible? boolean?]]]]
   [:window renderer.window.db/window]
   [:theme renderer.theme.db/theme]
   [:timeline renderer.timeline.db/timeline]])

(def default
  {:tool :select
   :pointer-pos [0 0]
   :zoom-sensitivity 0.75
   :state :default
   :documents {}
   :document-tabs []
   :dialogs []
   :recent []
   :system-fonts []
   :notifications []
   :debug-info? false
   :pen-mode? false
   :backdrop? false
   :rulers-locked? false
   :grid-visible? false
   :rulers-visible? true
   :snap {:enabled? true
          :threshold 100
          :options #{:centers :midpoints :corners :nodes}}
   :lang :en-US
   :repl-mode :cljs
   :theme {:mode :dark}
   :worker {:tasks #{}}
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
