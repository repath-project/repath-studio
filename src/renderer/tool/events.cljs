(ns renderer.tool.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.element.events :as element.e]
   [renderer.frame.handlers :as frame.h]
   [renderer.tool.handlers :as h]))

(rf/reg-event-fx
 ::activate
 (fn [{:keys [db]} [_ tool]]
   {:db (h/activate db tool)
    ::app.fx/focus nil}))

(rf/reg-event-fx
 ::pointer-event
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ e]]
   {:db (h/pointer-handler db e now)}))

(rf/reg-event-db
 ::wheel-event
 (fn [db [_ e]]
   (h/wheel-handler db e)))

(rf/reg-event-fx
 ::drag-event
 (fn [{:keys [db]} [_ {:keys [data-transfer pointer-pos] :as e}]]
   (when (= (:type e) "drop")
     {::app.fx/data-transfer [(frame.h/adjust-pointer-pos db pointer-pos) data-transfer]})))

(rf/reg-event-db
 ::keyboard-event
 (fn [db [_ e]]
   (h/key-handler db e)))

(rf/reg-event-fx
 ::cancel
 (fn [{:keys [db]} _]
   (if (and (= (:tool db) :transform)
            (= (:state db) :idle))
     {:dispatch [::element.e/deselect-all]}
     {:db (h/cancel db)})))
