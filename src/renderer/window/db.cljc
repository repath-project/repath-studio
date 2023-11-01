(ns renderer.window.db)

(def sidebar
  [:map
   [:size [int? {:default 300}]]
   [:visible? boolean?]])

(def window
  [:map
   [:maximized? boolean?]
   [:minimized? boolean?]
   [:fullscreen? boolean?]
   [:header? boolean?]
   [:history? boolean?]
   [:timeline? boolean?]
   [:xml? boolean?]
   [:tree sidebar]
   [:properties sidebar]
   [:elements-collapsed? boolean?]
   [:pages-collapsed? boolean?]
   [:command-palette? boolean?]
   [:repl-history? boolean?]
   [:theme-mode [:enum :dark :light :system]]])