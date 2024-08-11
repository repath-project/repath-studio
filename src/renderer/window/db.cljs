(ns renderer.window.db)

(def window
  [:map {:default {}}
   [:maximized? [boolean? {:default true}]]
   [:minimized? [boolean? {:default false}]]
   [:fullscreen? [boolean? {:default false}]]])
