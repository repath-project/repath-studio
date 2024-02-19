(ns renderer.window.db)

(def window
  [:map
   [:maximized? boolean?]
   [:minimized? boolean?]
   [:fullscreen? boolean?]
   [:size [:tuple double? double?]]])
