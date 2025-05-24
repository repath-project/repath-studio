(ns renderer.event.events
  (:require
   [re-frame.core :as rf]
   [renderer.event.handlers :as event.handlers]))

(rf/reg-event-db
 ::pointer
 (fn [db [_ e]]
   (event.handlers/pointer db e)))

(rf/reg-event-db
 ::wheel
 (fn [db [_ e]]
   (event.handlers/wheel db e)))

(rf/reg-event-db
 ::drag
 (fn [db [_ e]]
   (event.handlers/drag db e)))

(rf/reg-event-db
 ::keyboard
 (fn [db [_ e]]
   (event.handlers/keyboard db e)))
