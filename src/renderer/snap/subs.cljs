(ns renderer.snap.subs
  (:require
   [re-frame.core :as rf]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.subs :as-alias frame.subs]))

(rf/reg-sub
 ::snap
 :-> :snap)

(rf/reg-sub
 ::active?
 :<- [::snap]
 :-> :active)

(rf/reg-sub
 ::options
 :<- [::snap]
 :-> :options)

(rf/reg-sub
 ::nearest-neighbor
 :-> :nearest-neighbor)
