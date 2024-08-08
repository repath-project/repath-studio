(ns renderer.theme.db)

(def theme
  [:map
   [:mode [:enum :dark :light :system]]
   [:native [:enum :dark :light]]])
