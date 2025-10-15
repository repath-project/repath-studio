(ns renderer.worker.events
  (:require
   [re-frame.core :as rf]
   [renderer.effects :as-alias effects]
   [renderer.worker.effects :as-alias worker.effects]))

(rf/reg-event-fx
 ::create
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} [_ options]]
   {:db (assoc-in db [:worker :tasks guid] (:action options))
    ::worker.effects/post (-> options
                              (assoc-in [:data :id] (str guid))
                              (assoc-in [:data :action] (:action options)))}))

(rf/reg-event-fx
 ::message
 (fn [{:keys [db]} [_ id event response-data]]
   (cond-> {:db (update-in db [:worker :tasks] dissoc id)}
     event
     (assoc :dispatch (conj event response-data)))))
