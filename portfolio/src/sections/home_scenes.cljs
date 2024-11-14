(ns sections.home-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.app.subs]
   [renderer.app.views :as app.v]))

(defscene home
  :title "Home"
  :params (atom ["path/to/file/name.rps"])
  [store]
  [:div.flex.flex-col.h-dvh.overflow-hidden
   [app.v/home @store]])
