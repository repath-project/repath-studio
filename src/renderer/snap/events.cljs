(ns renderer.snap.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-db
 ::toggle
 (fn [db [_]]
   (update-in db [:snap :enabled?] not)))

(rf/reg-event-db
 ::toggle-option
 (fn [{:keys [snap] :as db} [_ option]]
   (if (contains? (:options snap) option)
     (update-in db [:snap :options] disj option)
     (update-in db [:snap :options] conj option))))
