(ns renderer.theme.db)

(def Theme
  [:map {:default {} :closed true}
   [:mode {:default :dark} [:enum :dark :light :system]]
   [:native {:optional true} [:enum :dark :light]]])
