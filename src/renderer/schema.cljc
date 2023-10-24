(ns renderer.schema
  (:require
   [renderer.window.schema]
   [renderer.document.schema]))

(def db
  [:map
   [:tool keyword?]
   [:mouse-pos [:tuple double? double?]]
   [:zoom-factor double?]
   [:state keyword?]
   [:documents [:map-of :uuid renderer.document.schema/document]]
   [:document-tabs [:vector uuid?]]
   [:system-fonts vector?]
   [:debug-info? boolean?]
   [:pen-mode? boolean?]
   [:window renderer.window.schema/window]])