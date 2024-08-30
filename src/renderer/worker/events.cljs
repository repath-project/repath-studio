(ns renderer.worker.events
  (:require
   [re-frame.core :as rf]
   [renderer.worker.effects :as fx]))

(rf/reg-event-fx
 ::create
 (fn [{:keys [db]} [_ {:keys [action] :as options}]]
   (let [task-id (random-uuid)]
     {:db (assoc-in db [:worker :tasks task-id] action)
      ::fx/post (assoc-in options [:data :id] task-id)})))

(rf/reg-event-db
 ::completed
 (fn [db [_ id]]
   (update-in db [:worker :tasks] dissoc id)))
