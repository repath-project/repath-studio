(ns sections.home-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.app.subs]
   [renderer.app.views :as app.v]))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defscene home
  :title "Home"
  :params (atom ["asdsads"])
  [store]
  [:div.flex.flex-col.h-dvh.overflow-hidden
   [app.v/home @store]])
