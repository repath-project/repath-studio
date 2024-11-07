(ns renderer.window.db)

(def Window
  [:map {:closed true}
   [:maximized {:default true} boolean?]
   [:minimized {:default false} boolean?]
   [:fullscreen {:default false} boolean?]
   [:focused {:default false} boolean?]])
