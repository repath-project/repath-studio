(ns renderer.history.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as app.fx]
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
 ::reset-state
 h/reset-state)

(rf/reg-event-db
 ::preview
 (fn [db [_ pos]]
   (h/preview db pos)))

(rf/reg-event-db
 ::go-to
 (fn [db [_ id]]
   (h/go-to db id)))

(rf/reg-event-db
 ::clear
 (fn [db _]
   (-> (h/clear db)
       (h/finalize "Clear history"))))

(rf/reg-event-db
 ::tree-view-updated
 (fn [db [_ zoom translate]]
   (cond-> db
     zoom
     (h/set-zoom zoom)

     translate
     (h/set-translate translate))))

(rf/reg-global-interceptor
 (rf/->interceptor
  :id ::auto-persist
  :after (fn [context]
           (let [db (rf/get-effect context :db)
                 fx (rf/get-effect context :fx)
                 prev-position (when-let [db (rf/get-coeffect context :db)]
                                 (when (:active-document db)
                                   (h/position db)))]
             (cond-> context
               (and db (not= (h/position db) prev-position))
               (rf/assoc-effect :fx (conj (or fx []) [::app.fx/persist db])))))))
