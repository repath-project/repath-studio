(ns renderer.window.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::window
 :-> :window)

(rf/reg-sub
 ::maximized?
 :<- [::window]
 :-> :maximized)

(rf/reg-sub
 ::fullscreen?
 :<- [::window]
 :-> :fullscreen)

(rf/reg-sub
 ::focused?
 :<- [::window]
 :-> :focused)

(rf/reg-sub
 ::width
 :<- [::window]
 :-> :width)

(rf/reg-sub
 ::breakpoint?
 :<- [::width]
 (fn [width [_ breakpoint]]
   ;; https://tailwindcss.com/docs/responsive-design#overview
   (>= width (get {:2xl 1536
                   :xl 1280
                   :lg 1024
                   :md 768
                   :sm 640} breakpoint))))
