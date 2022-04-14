(ns repath.studio.window.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :window/tree?
 (fn [db _]
   (-> db :window :tree?)))

(rf/reg-sub
 :window/properties?
 (fn [db _]
   (-> db :window :properties?)))

(rf/reg-sub
 :window/header?
 (fn [db _]
   (-> db :window :header?)))

(rf/reg-sub
 :window/xml?
 (fn [db _]
   (-> db :window :xml?)))


(rf/reg-sub
 :window/timeline?
 (fn [db _]
   (-> db :window :timeline?)))

(rf/reg-sub
 :window/history?
 (fn [db _]
   (-> db :window :history?)))

(rf/reg-sub
 :window/minimized?
 (fn [db _]
   (-> db :window :minimized?)))

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
 :window/repl-history-collapsed?
 (fn [db _]
   (-> db :window :repl-history-collapsed?)))

(rf/reg-sub
 :window/symbols-collapsed?
 (fn [db _]
   (-> db :window :symbols-collapsed?)))

(rf/reg-sub
 :window/left-sidebar-width
 (fn [db _]
   (-> db :window :left-sidebar-width)))

(rf/reg-sub
 :window/right-sidebar-width
 (fn [db _]
   (-> db :window :right-sidebar-width)))