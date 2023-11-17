(ns renderer.tree.events
  (:require
   [re-frame.core :as rf]
   [renderer.utils.local-storage :as local-storage]))

(rf/reg-event-db
 :tree/toggle-elements-collapsed
 [local-storage/persist
  (rf/path :tree)]
 (fn [db [_]]
   (update db :elements-collapsed? not)))

(rf/reg-event-db
 :tree/toggle-pages-collapsed
 [local-storage/persist
  (rf/path :tree)]
 (fn [db [_]]
   (update db :pages-collapsed? not)))
