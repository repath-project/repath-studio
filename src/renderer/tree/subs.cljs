(ns renderer.tree.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :tree/elements-collapsed?
 (fn [db _]
   (-> db :tree :elements-collapsed?)))

(rf/reg-sub
 :tree/pages-collapsed?
 (fn [db _]
   (-> db :tree :pages-collapsed?)))

(rf/reg-sub
 :tree/defs-collapsed?
 (fn [db _]
   (-> db :tree :defs-collapsed?)))

(rf/reg-sub
 :tree/symbols-collapsed?
 (fn [db _]
   (-> db :tree :symbols-collapsed?)))