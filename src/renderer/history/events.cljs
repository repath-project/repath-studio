(ns renderer.history.events
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.e]
   [renderer.history.handlers :as h]))

(rf/reg-event-db
 ::undo
 (fn [db _]
   (h/undo db)))

(rf/reg-event-db
 ::redo
 (fn [db _]
   (h/redo db)))

(rf/reg-event-db
 ::undo-by
 (fn [db [_ n]]
   (h/undo db n)))

(rf/reg-event-db
 ::redo-by
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
 (fn [db [_ id]]
   (h/move db id)))

(rf/reg-event-db
 ::clear
 (fn [db _]
   (-> (h/clear db)
       (h/finalize "Clear history"))))

(rf/reg-event-db
 ::tree-view-updated
 (fn [db [_ zoom translate]]
   (cond-> db
     zoom (h/set-zoom zoom)
     translate (h/set-translate translate))))


