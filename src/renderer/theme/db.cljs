(ns renderer.theme.db)

(def theme
  [:map
   [:mode [:enum :dark :light :system]]
   [:native {:optional true} [:enum :dark :light]]])
