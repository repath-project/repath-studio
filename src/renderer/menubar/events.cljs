(ns renderer.menubar.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-db
 ::activate
 (fn [db [_ k]]
   (-> db
       (assoc-in [:menubar :indicator] false)
       (assoc-in [:menubar :active] k)
       (assoc :backdrop true))))

(rf/reg-event-db
 ::deactivate
 (fn [db [_]]
   (-> db
       (assoc-in [:menubar :indicator] false)
       (update :menubar dissoc :active)
       (assoc :backdrop false))))
