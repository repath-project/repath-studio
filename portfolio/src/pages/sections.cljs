(ns pages.sections
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.app.subs]
   [renderer.app.views :as app.views]))

(defscene ^:export home
  :title "Home"
  :params (atom [{:path "path/to/file/name.rps"}])
  [store]
  [:div.flex.flex-col.h-dvh.overflow-hidden
   [app.views/home @store]])
