(ns renderer.window.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::window
 :-> :window)

(rf/reg-sub
 ::maximized?
 :<- [::window]
 :-> :maximized?)

(rf/reg-sub
 ::fullscreen?
 :<- [::window]
 :-> :fullscreen?)
