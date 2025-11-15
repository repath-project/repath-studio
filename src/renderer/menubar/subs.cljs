(ns renderer.menubar.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::menubar
 :-> :menubar)

(rf/reg-sub
 ::indicator?
 :<- [::menubar]
 :-> :indicator)

(rf/reg-sub
 ::active-menu
 :<- [::menubar]
 :-> :active)
