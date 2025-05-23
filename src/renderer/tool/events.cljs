(ns renderer.tool.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.element.events :as element.events]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.tool.effects :as-alias tool.effects]
   [renderer.tool.handlers :as tool.handlers]))

(rf/reg-event-fx
 ::activate
 (fn [{:keys [db]} [_ tool]]
   {:db (tool.handlers/activate db tool)
    ::app.effects/focus nil}))

(rf/reg-event-db
 ::set-state
 (fn [db [_ state]]
   (tool.handlers/set-state db state)))

(rf/reg-event-fx
 ::pointer-event
 (fn [{:keys [db]} [_ e]]
   {:db (tool.handlers/pointer-handler db e)}))

(rf/reg-event-db
 ::wheel-event
 (fn [db [_ e]]
   (tool.handlers/wheel-handler db e)))

(rf/reg-event-fx
 ::drag-event
 (fn [{:keys [db]} [_ {:keys [data-transfer pointer-pos] :as e}]]
   (when (= (:type e) "drop")
     (let [position (frame.handlers/adjusted-pointer-pos db pointer-pos)]
       {::tool.effects/drop [position data-transfer]}))))

(rf/reg-event-db
 ::keyboard-event
 (fn [db [_ e]]
   (tool.handlers/key-handler db e)))

(rf/reg-event-fx
 ::cancel
 (fn [{:keys [db]} _]
   (if (and (= (:tool db) :transform)
            (= (:state db) :idle))
     {:dispatch [::element.events/deselect-all]}
     {:db (tool.handlers/cancel db)})))

(rf/reg-global-interceptor
 (rf/->interceptor
  :id ::custom-fx
  :after (fn [context]
           (let [db (rf/get-effect context :db)
                 fx (rf/get-effect context :fx)]
             (cond-> context
               db
               (-> (rf/assoc-effect :fx (apply conj (or fx []) (:fx db)))
                   (rf/assoc-effect :db (assoc db :fx []))))))))
