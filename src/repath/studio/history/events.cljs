(ns repath.studio.history.events
  (:require [re-frame.core :as rf]
            [repath.studio.history.handlers :as h]
            [repath.studio.elements.handlers :as elements]))

(rf/reg-event-db
 :history/undo
 (fn [db [_ n]]
   (h/undo db n)))

(rf/reg-event-db
 :history/redo
 (fn [db [_ n]]
    (h/redo db n)))

(rf/reg-event-db
 :history/finalize
 (fn [db [_ explanation]]
   (h/finalize db explanation)))

(rf/reg-event-db
 :history/cancel
 (fn [db _]
   (case (:state db)
     :default (elements/deselect-all db)
     (-> db
         (elements/clear-temp)
         (dissoc :mouse-offset)
         (assoc :state :default)
         (h/swap)
         (assoc :state :default)))))