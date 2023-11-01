(ns renderer.window.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :window/sidebar?
 (fn [db [_ key]]
   (-> db :window key :visible?)))

(rf/reg-sub
 :window/sidebar
 (fn [db [_ key]]
   (-> db :window key :size)))

(rf/reg-sub
 :window/header?
 (fn [db _]
   (-> db :window :header?)))

(rf/reg-sub
 :window/repl-history?
 (fn [db _]
   (-> db :window :repl-history?)))

#_(rf/reg-sub
   :window/timeline?
   (fn [db _]
     (-> db :window :timeline?)))

(rf/reg-sub
 :window/maximized?
 (fn [db _]
   (-> db :window :maximized?)))

(rf/reg-sub
 :window/fullscreen?
 (fn [db _]
   (-> db :window :fullscreen?)))

(rf/reg-sub
 :window/elements-collapsed?
 (fn [db _]
   (-> db :window :elements-collapsed?)))

(rf/reg-sub
 :window/pages-collapsed?
 (fn [db _]
   (-> db :window :pages-collapsed?)))

(rf/reg-sub
 :window/defs-collapsed?
 (fn [db _]
   (-> db :window :defs-collapsed?)))

(rf/reg-sub
 :window/symbols-collapsed?
 (fn [db _]
   (-> db :window :symbols-collapsed?)))

(rf/reg-sub
 :window/drag
 (fn [db _]
   (-> db :window :drag)))