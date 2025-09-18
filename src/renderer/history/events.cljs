(ns renderer.history.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as app.effects]
   [renderer.history.handlers :as history.handlers]))

(rf/reg-event-db
 ::undo
 (fn [db _]
   (history.handlers/undo db)))

(rf/reg-event-db
 ::redo
 (fn [db _]
   (history.handlers/redo db)))

(rf/reg-event-db
 ::undo-by
 (fn [db [_ n]]
   (history.handlers/undo db n)))

(rf/reg-event-db
 ::redo-by
 (fn [db [_ n]]
   (history.handlers/redo db n)))

(rf/reg-event-db
 ::reset-state
 (fn [db _]
   (-> (history.handlers/reset-state db)
       (update-in [:documents (:active-document db)] dissoc :preview-label))))

(rf/reg-event-db
 ::preview
 (fn [db [_ pos]]
   (history.handlers/preview db pos)))

(rf/reg-event-db
 ::go-to
 (fn [db [_ id]]
   (-> (history.handlers/go-to db id)
       (update-in [:documents (:active-document db)] dissoc :preview-label))))

(rf/reg-event-db
 ::clear
 (fn [db _]
   (-> (history.handlers/clear db)
       (history.handlers/finalize [::clear-history "Clear history"]))))

(rf/reg-event-db
 ::tree-view-updated
 (fn [db [_ zoom translate]]
   (cond-> db
     zoom
     (history.handlers/set-zoom zoom)

     translate
     (history.handlers/set-translate translate))))

(rf/reg-global-interceptor
 (rf/->interceptor
  :id ::auto-persist
  :after (fn [context]
           (let [db (rf/get-effect context :db)
                 fx (rf/get-effect context :fx)
                 prev-position (when-let [db (rf/get-coeffect context :db)]
                                 (when (:active-document db)
                                   (history.handlers/position db)))]
             (cond-> context
               (and db (not= (history.handlers/position db) prev-position))
               (rf/assoc-effect :fx (conj (or fx []) [::app.effects/persist])))))))
