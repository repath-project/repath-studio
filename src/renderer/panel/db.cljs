(ns renderer.panel.db)

(def panel
  [:map
   [:size [int? {:default 300}]]
   [:visible? boolean?]])