(ns renderer.worker.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.worker.effects :as fx]))

(rf/reg-event-fx
 ::create
 [(rf/inject-cofx ::app.fx/random-uuid)]
 (fn [{:keys [db random-uuid]} [_ {:keys [action] :as options}]]
   {:db (assoc-in db [:worker :tasks random-uuid] action)
    ::fx/post (assoc-in options [:data :id] random-uuid)}))

(rf/reg-event-db
 ::completed
 (fn [db [_ id]]
   (update-in db [:worker :tasks] dissoc id)))
