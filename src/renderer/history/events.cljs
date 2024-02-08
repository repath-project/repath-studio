(ns renderer.history.events
  (:require
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as h]
   [renderer.tools.base :as tools]))

(rf/reg-event-db
 :history/undo
 (fn [db _]
   (h/undo db 1)))

(rf/reg-event-db
 :history/redo
 (fn [db _]
   (h/redo db 1)))

(rf/reg-event-db
 :history/undo-by
 (fn [db [_ n]]
   (h/undo db n)))

(rf/reg-event-db
 :history/redo-by
 (fn [db [_ n]]
   (h/redo db n)))

(rf/reg-event-db
 :history/cancel
 (fn [db _]
   (cond-> db
     (and (= (:tool db) :select) (= (:state db) :default))
     (-> element.h/deselect
         (h/finalize "Deselect all"))

     (= (:state db) :select)
     (element.h/clear-hovered)

     (= (:state db) :default)
     (tools/set-tool :select)

     :always (-> (dissoc :pointer-offset)
                 (dissoc :drag?)
                 (assoc :state :default)
                 element.h/clear-temp
                 h/swap))))
