(ns renderer.history.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :refer [persist]]
   [renderer.element.events :as-alias element.e]
   [renderer.history.handlers :as h]))

(rf/reg-event-db
 ::undo
 [persist]
 (fn [db _]
   (h/undo db)))

(rf/reg-event-db
 ::redo
 [persist]
 (fn [db _]
   (h/redo db)))

(rf/reg-event-db
 ::undo-by
 [persist]
 (fn [db [_ n]]
   (h/undo db n)))

(rf/reg-event-db
 ::redo-by
 [persist]
 (fn [db [_ n]]
   (h/redo db n)))

(rf/reg-event-db
 ::swap
 h/swap)

(rf/reg-event-db
 ::preview
 (fn [db [_ pos]]
   (h/preview db pos)))

(rf/reg-event-db
 ::move
 [persist]
 (fn [db [_ id]]
   (h/move db id)))

(rf/reg-event-db
 ::clear
 [(h/finalize "Clear history")]
 h/clear)

(rf/reg-event-db
 ::tree-view-updated
 (fn [db [_ zoom translate]]
   (cond-> db
     zoom (h/set-zoom zoom)
     translate (h/set-translate translate))))

(rf/reg-event-fx
 ::cancel
 (fn [{:keys [db]} _]
   (if (and (= (:tool db) :transform)
            (= (:state db) :idle))
     {:dispatch [::element.e/deselect-all]}
     {:db (h/cancel db)})))

