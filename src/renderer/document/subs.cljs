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
 :document/elements
 :<- [:document/active]
 :-> :elements)

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
 :document/collapsed-keys
 :<- [:document/active]
 :-> :collapsed-keys)

(rf/reg-sub
 :document/ignored-keys
 :<- [:document/active]
 :-> :ignored-keys)

(rf/reg-sub
 :document/read-only?
 :<- [:timeline/time]
 (fn [time _]
   (pos? time)))
