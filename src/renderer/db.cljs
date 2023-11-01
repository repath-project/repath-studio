(ns renderer.db
  (:require
   [renderer.window.db]
   [renderer.document.db]))

(def app
  [:map
   [:tool keyword?]
   [:mouse-pos [:tuple double? double?]]
   [:zoom-factor double?]
   [:state keyword?]
   [:documents [:map-of :uuid renderer.document.db/document]]
   [:document-tabs [:vector uuid?]]
   [:system-fonts vector?]
   [:debug-info? boolean?]
   [:pen-mode? boolean?]
   [:window renderer.window.db/window]])

(def default
  {:tool :select
   :mouse-pos [0 0]
   :zoom-factor 0.8
   :state :default
   :documents {}
   :document-tabs []
   :system-fonts []
   :notifications []
   :debug-info? false
   :pen-mode? false
   :repl/mode :cljs
   :window {:maximized? true
            :minimized? false
            :fullscreen? false
            :header? true
            :history? false
            :timeline? true
            :xml? false
            :tree {:size 300
                   :visible? true}
            :properties {:size 300
                         :visible? true}
            :elements-collapsed? false
            :pages-collapsed? false
            :command-palette? false
            :defs-collapsed? true
            :symbols-collapsed? true
            :repl-history? false
            :theme-mode :dark}})
