(ns renderer.history.events
  (:require
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as h]
   [renderer.tool.base :as tool]))

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
 :history/swap
 (fn [db _]
   (h/swap db)))

(rf/reg-event-db
 :history/preview
 (fn [db [_ id]]
   (h/preview db id)))

(rf/reg-event-db
 :history/move
 (fn [db [_ id]]
   (h/move db id)))

(rf/reg-event-db
 :history/clear
 (fn [db _]
   (h/clear db)))

(rf/reg-event-db
 :history/tree-view-updated
 (fn [db [_ zoom translate]]
   (-> db
       (h/set-zoom zoom)
       (h/set-translate translate))))

(rf/reg-event-db
 :history/cancel
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
     (tool/set-tool :select)))) ; FIXME

(rf/reg-event-fx
 :history/restore
 (fn [{:keys [db]} _]
   {:db (if (:restored? db)
          (-> db
              (h/mark-restored)
              (h/undo 1)
              (dissoc :restored?))
          (-> db
              element.h/clear-hovered
              (tool/set-tool :select)
              (dissoc :pointer-offset)
              (dissoc :clicked-element)
              (dissoc :drag?)
              (assoc :state :default)
              (assoc :restored? true)
              element.h/clear-temp
              h/swap))
    :dispatch ^:flush-dom [:clear-restored]}))
