(ns renderer.snap.subs
  (:require
   [re-frame.core :as rf]))

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
