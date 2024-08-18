(ns renderer.window.db)

(def window
  [:map {:default {}}
   [:maximized? {:default true} boolean?]
   [:minimized? {:default false} boolean?]
   [:fullscreen? {:default false} boolean?]
   [:focused? {:default false} boolean?]])
