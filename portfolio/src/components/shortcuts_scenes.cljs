(ns components.shortcuts-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [renderer.ui :as ui]
   [renderer.utils.keyboard :as keyb]))

(defscene single-shortcut
  :title "Single shortcut"
  [:div.toolbar.bg-primary.h-10.p-2
   [ui/shortcuts [[{:keyCode (keyb/key-codes "P")
                    :ctrlKey true
                    :shiftKey true}]]]])

(defscene multiple-shortcuts
  :title "Multiple shortcuts"
  [:div.toolbar.bg-primary.h-10.p-2
   [ui/shortcuts [[{:keyCode (keyb/key-codes "P")
                    :ctrlKey true
                    :shiftKey true}]
                  [{:keyCode (keyb/key-codes "ONE")
                    :altKey true}]]]])

(defscene no-shortcuts
  :title "No shortcuts"
  [:div.toolbar.bg-primary.h-10.p-2
   [ui/shortcuts []]])
