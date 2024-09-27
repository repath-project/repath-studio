(ns renderer.ruler.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as fx :refer [persist]]))

  (rf/reg-event-db
   ::toggle-visible
   persist
   (fn [db [_]]
     (update-in db [:ruler :visible] not)))

  (rf/reg-event-db
   ::toggle-locked
   (fn [db [_]]
     (update-in db [:ruler :locked] not)))
