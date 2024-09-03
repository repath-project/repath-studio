(ns renderer.snap.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]))

(rf/reg-event-db
 ::toggle
 [persist]
 (fn [db [_]]
   (update-in db [:snap :enabled?] not)))

(rf/reg-event-db
 ::toggle-option
 [persist]
 (fn [{:keys [snap] :as db} [_ option]]
   (if (contains? (:options snap) option)
     (update-in db [:snap :options] disj option)
     (update-in db [:snap :options] conj option))))
