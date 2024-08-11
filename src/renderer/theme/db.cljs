(ns renderer.theme.db)

(def theme
  [:map {:default {}}
   [:mode {:default :dark} [:enum :dark :light :system]]
   [:native {:optional true} [:enum :dark :light]]])
