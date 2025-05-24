(ns renderer.event.events
  (:require
   [re-frame.core :as rf]
   [renderer.event.handlers :as event.handlers]))

(rf/reg-event-fx
 ::pointer
 (fn [{:keys [db]} [_ e]]
   {:db (event.handlers/pointer db e)}))

(rf/reg-event-db
 ::wheel
 (fn [db [_ e]]
   (event.handlers/wheel db e)))

(rf/reg-event-fx
 ::drag
 (fn [{:keys [db]} [_ e]]
   (event.handlers/drag db e)))

(rf/reg-event-db
 ::keyboard
 (fn [db [_ e]]
   (event.handlers/keyboard db e)))
