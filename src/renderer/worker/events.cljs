(ns renderer.worker.events
  (:require
   [re-frame.core :as rf]
   [renderer.utils.uuid :as uuid]))

(rf/reg-fx
 ::post
 (fn [{:keys [worker data callback]}]
   (let [worker (js/Worker. (str "js/" worker))]
     (.. worker (addEventListener "message" callback))
     (.postMessage worker (clj->js data)))))

(rf/reg-event-fx
 ::create
 (fn [{:keys [db]} [_ {:keys [action] :as options}]]
   (let [task-id (uuid/generate)]
     {:db (assoc-in db [:worker :tasks task-id] action)
      ::post (assoc-in options [:data :id] task-id)})))

(rf/reg-event-db
 ::completed
 (fn [db [_ id]]
   (update-in db [:worker :tasks] dissoc id)))
