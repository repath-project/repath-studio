(ns repath.studio.history.events
  (:require [re-frame.core :as rf]
            [repath.studio.handlers :as handlers]
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
 :history/cancel
 (fn [db _]
   (cond-> db
     (= (:state db) :default) (elements/deselect-all)
     (= (:state db) :create) (->
                              (handlers/set-tool :select)
                              (assoc :state :default))
     :always (-> (elements/clear-temp)
                 (dissoc :mouse-offset)
                 (h/swap)))))