(ns renderer.window.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :window
 :-> :window)

(rf/reg-sub
 :window/maximized?
 :<- [:window]
 :-> :maximized?)

(rf/reg-sub
 :window/fullscreen?
 :<- [:window]
 :-> :fullscreen?)
