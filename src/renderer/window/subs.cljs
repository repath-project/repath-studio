(ns renderer.window.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :window
 :-> :window)

(rf/reg-sub
 :window/header?
 :<- [:window]
 :-> :header?)

(rf/reg-sub
 :window/maximized?
 :<- [:window]
 :-> :maximized?)

(rf/reg-sub
 :window/fullscreen?
 :<- [:window]
 :-> :fullscreen?)

(rf/reg-sub
 :window/size
 :<- [:window]
 :-> :size)

(rf/reg-sub
 :window/left-sidebar-min-width
 :<- [:window/size]
 (fn [size]
   (when-let [width (first size)]
     (* (/ 230 width) 100))))

(rf/reg-sub
 :window/right-sidebar-min-width
 :<- [:window/size]
 (fn [size]
   (when-let [width (first size)]
     (* (/ 300 width) 100))))
