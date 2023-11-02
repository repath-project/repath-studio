(ns renderer.document.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :document/active
 :<- [:documents]
 :<- [:active-document]
 (fn [[documents active-document] _]
   (when active-document
     (active-document documents))))

(rf/reg-sub
 :document/zoom
 :<- [:document/active]
 :-> :zoom)

(rf/reg-sub
 :document/rotate
 :<- [:document/active]
 :-> :rotate)

(rf/reg-sub
 :document/fill
 :<- [:document/active]
 :-> :fill)

(rf/reg-sub
 :document/stroke
 :<- [:document/active]
 :-> :stroke)

(rf/reg-sub
 :document/pan
 :<- [:document/active]
 :-> :pan)

(rf/reg-sub
 :document/title
 :<- [:document/active]
 :-> :title)

(rf/reg-sub
 :document/history
 :<- [:document/active]
 :-> :history)

(rf/reg-sub
 :document/elements
 :<- [:document/active]
 :-> :elements)

(rf/reg-sub
 :document/active-page
 :<- [:document/active]
 :-> :active-page)

(rf/reg-sub
 :document/temp-element
 :<- [:document/active]
 :-> :temp-element)

(rf/reg-sub
 :document/filter
 :<- [:document/active]
 :-> :filter)

(rf/reg-sub
 :document/hovered-keys
 :<- [:document/active]
 :-> :hovered-keys)

(rf/reg-sub
 :document/ignored-keys
 :<- [:document/active]
 :-> :ignored-keys)

#_(rf/reg-sub
   :document/rulers-locked?
   :<- [:document/active]
   :-> :rulers-locked?)

(rf/reg-sub
 :document/rulers?
 :<- [:document/active]
 :-> :rulers?)

(rf/reg-sub
 :document/xml?
 :<- [:document/active]
 :-> :xml?)

(rf/reg-sub
 :document/history?
 :<- [:document/active]
 :-> :history?)

(rf/reg-sub
 :document/grid?
 :<- [:document/active]
 :-> :grid)

#_(rf/reg-sub
   :document/snap?
   :<- [:document/active]
   :-> :snap?)