(ns renderer.worker.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.worker.effects :as fx]))

(rf/reg-event-fx
 ::create
 [(rf/inject-cofx ::app.fx/guid)]
 (fn [{:keys [db guid]} [_ {:keys [action] :as options}]]
   {:db (assoc-in db [:worker :tasks guid] action)
    ::fx/post (assoc-in options [:data :id] guid)}))

(rf/reg-event-db
 ::completed
 (fn [db [_ id]]
   (update-in db [:worker :tasks] dissoc id)))
