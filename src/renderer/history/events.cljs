(ns renderer.history.events
  (:require
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as handlers]
   [renderer.history.handlers :as h]
   [renderer.utils.local-storage :as local-storage]))

(rf/reg-event-db
 ::undo
 local-storage/persist
 (fn [db _]
   (h/undo db)))

(rf/reg-event-db
 ::redo
 local-storage/persist
 (fn [db _]
   (h/redo db)))

(rf/reg-event-db
 ::undo-by
 local-storage/persist
 (fn [db [_ n]]
   (h/undo db n)))

(rf/reg-event-db
 ::redo-by
 local-storage/persist
 (fn [db [_ n]]
   (h/redo db n)))

#_(rf/reg-event-db
   ::swap
   (fn [db _]
     (h/swap db)))

(rf/reg-event-db
 ::move
 local-storage/persist
 (fn [db [_ id]]
   (h/move db id)))

(rf/reg-event-db
 ::clear
 local-storage/persist
 (fn [db _]
   (h/clear db)))

(rf/reg-event-db
 ::tree-view-updated
 (fn [db [_ zoom translate]]
   (-> db
       (h/set-zoom zoom)
       (h/set-translate translate))))

(rf/reg-event-db
 ::cancel
 (fn [db _]
   (cond-> db
     :always (-> (dissoc :pointer-offset)
                 (dissoc :drag?)
                 (assoc :state :default)
                 element.h/clear-temp
                 h/swap)

     (and (= (:tool db) :select) (= (:state db) :default))
     (-> element.h/deselect
         (h/finalize "Deselect all"))

     (= (:state db) :select)
     (element.h/clear-hovered)

     (= (:state db) :default)
     (handlers/set-tool :select)))) ; FIXME

(rf/reg-event-fx
 ::restore
 local-storage/persist
 (fn [{:keys [db]} _]
   {:db (if (:restored? db)
          (-> db
              h/mark-restored
              h/undo
              (dissoc :restored?))
          (-> db
              element.h/clear-hovered
              (handlers/set-tool :select)
              (dissoc :pointer-offset)
              (dissoc :clicked-element)
              (dissoc :drag?)
              (assoc :state :default)
              (assoc :restored? true)
              element.h/clear-temp
              h/swap))
    :dispatch ^:flush-dom [:clear-restored]}))
