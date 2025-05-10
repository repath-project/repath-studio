(ns components.shortcuts-scenes
  (:require
   [portfolio.reagent-18 :refer-macros [defscene]]
   [re-frame.core :as rf]
   [re-pressed.core :as rp]
   [renderer.history.events :as-alias history.events]
   [renderer.ui :as ui]
   [renderer.utils.keyboard :as utils.keyboard]))

(rf/dispatch [::rp/set-keydown-rules utils.keyboard/keydown-rules])

(defscene single-shortcut
  :title "Single shortcut"
  [:div.toolbar.bg-primary.h-10.p-2.gap-2
   "Undo"
   [ui/shortcuts [::history.events/undo]]])

(defscene multiple-shortcuts
  :title "Multiple shortcuts"
  [:div.toolbar.bg-primary.h-10.p-2.gap-2
   "Redo"
   [ui/shortcuts [::history.events/redo]]])

(defscene no-shortcuts
  :title "No shortcuts"
  [:div.toolbar.bg-primary.h-10.p-2.gap-2
   "Event with no shortcuts"
   [ui/shortcuts [:event-id-with-no-shortcut]]])
