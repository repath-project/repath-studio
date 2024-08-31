(ns renderer.history.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as app.fx]
   [renderer.app.events :refer [persist]]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as h]
   [renderer.tool.base :as tool]))

(rf/reg-event-db
 ::undo
 persist
 (fn [db _]
   (h/undo db)))

(rf/reg-event-db
 ::redo
 persist
 (fn [db _]
   (h/redo db)))

(rf/reg-event-db
 ::undo-by
 persist
 (fn [db [_ n]]
   (h/undo db n)))

(rf/reg-event-db
 ::redo-by
 persist
 (fn [db [_ n]]
   (h/redo db n)))

(rf/reg-event-db
 ::swap
 (fn [db _]
   (h/swap db)))

(rf/reg-event-db
 ::preview
 (fn [db [_ pos]]
   (h/preview db pos)))

(rf/reg-event-db
 ::move
 persist
 (fn [db [_ id]]
   (h/move db id)))

(rf/reg-event-fx
 ::clear
 [(rf/inject-cofx ::app.fx/now) persist]
 (fn [{:keys [db now]} _]
   {:db (h/clear db now)}))

(rf/reg-event-db
 ::tree-view-updated
 (fn [db [_ zoom translate]]
   (-> db
       (h/set-zoom zoom)
       (h/set-translate translate))))

(rf/reg-event-fx
 ::cancel
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (cond-> db
          :always (-> (dissoc :drag? :pointer-offset)
                      (tool/activate (:tool db))
                      (element.h/clear-temp)
                      (h/swap))

          (and (= (:tool db) :select) (= (:state db) :default))
          (-> (element.h/deselect)
              (h/finalize now "Deselect all"))

          (= (:state db) :select)
          (element.h/clear-hovered)

          (= (:state db) :default)
          (app.h/set-tool :select))})) ; FIXME

