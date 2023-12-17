(ns renderer.db
  (:require
   [renderer.document.db]
   [renderer.panel.db]
   [renderer.theme.db]
   [renderer.tree.db]
   [renderer.window.db]))

(def app
  [:map
   [:tool keyword?]
   [:pointer-pos [:tuple double? double?]]
   [:zoom-sensitivity double?]
   [:state keyword?]
   [:grid? boolean?]
   [:rulers? boolean?]
   [:snap? boolean?]
   [:rulers-locked? boolean?]
   [:documents [:map-of :uuid renderer.document.db/document]]
   [:document-tabs [:vector uuid?]]
   [:system-fonts vector?]
   [:debug-info? boolean?]
   [:pen-mode? boolean?]
   [:tree renderer.tree.db/tree]
   [:panel [:map-of :key renderer.panel.db/panel]]
   [:window renderer.window.db/window]
   [:theme [:mode renderer.theme.db/modes]]])

(def default
  {:tool :select
   :pointer-pos [0 0]
   :zoom-sensitivity 0.75
   :state :default
   :documents {}
   :document-tabs []
   :system-fonts []
   :notifications []
   :debug-info? false
   :pen-mode? false
   :rulers-locked? false
   :grid? false
   :rulers? true
   :lang :en-US
   :repl-mode :cljs
   :theme {:mode :dark}
   :cmdk {:visible? false}
   :tree {:elements-collapsed? false
          :pages-collapsed? false
          :defs-collapsed? true
          :symbols-collapsed? true}
   :panel {:tree {:size 300
                  :visible? true}
           :properties {:size 300
                        :visible? true}
           :history? false
           :timeline {:visible? true}
           :xml {:visible? false}
           :repl-history {:visible? false}}
   :window {:maximized? true
            :minimized? false
            :fullscreen? false
            :header? true}
   :timeline {:time 0
              :paused? false}})
