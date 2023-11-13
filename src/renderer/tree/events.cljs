(ns renderer.tree.events
  (:require
   [re-frame.core :as rf]
   [akiroz.re-frame.storage :refer [persist-db-keys]]))

(rf/reg-event-db
 :tree/toggle-elements-collapsed
 [(persist-db-keys :repath [:tree :panel])
  (rf/path :tree)]
 (fn [db [_]]
   (update db :elements-collapsed? not)))

(rf/reg-event-db
 :tree/toggle-pages-collapsed
 [(persist-db-keys :repath [:tree :panel])
  (rf/path :tree)]
 (fn [db [_]]
   (update db :pages-collapsed? not)))

#_(rf/reg-event-db
   :tree/toggle-symbols-collapsed
   (rf/path :tree)
   (fn [db [_]]
     (update db :symbols-collapsed? not)))

#_(rf/reg-event-db
   :tree/toggle-defs-collapsed
   (rf/path :tree)
   (fn [db [_]]
     (update db :defs-collapsed? not)))
