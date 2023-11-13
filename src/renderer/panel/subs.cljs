(ns renderer.panel.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :panel/visible?
 (fn [db [_ key]]
   (-> db :panel key :visible?)))

(rf/reg-sub
 :panel/size
 (fn [db [_ key]]
   (-> db :panel key :size)))

(rf/reg-sub
 :panel/drag
 (fn [db _]
   (-> db :panel-state :drag)))
