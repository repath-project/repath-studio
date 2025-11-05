(ns renderer.menubar.events
  (:require
   [re-frame.core :as rf]
   [renderer.menubar.handlers :as menubar.handlers]))

(rf/reg-event-db
 ::activate
 (fn [db [_ k]]
   (menubar.handlers/activate db k)))

(rf/reg-event-db
 ::deactivate
 (fn [db [_]]
   (menubar.handlers/deactivate db)))
