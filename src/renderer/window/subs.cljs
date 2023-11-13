(ns renderer.window.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :window/header?
 (fn [db _]
   (-> db :window :header?)))

(rf/reg-sub
 :window/maximized?
 (fn [db _]
   (-> db :window :maximized?)))

(rf/reg-sub
 :window/fullscreen?
 (fn [db _]
   (-> db :window :fullscreen?)))
