(ns renderer.cmdk.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-db
 :cmdk/toggle
 (fn [db [_]]
   (update-in db [:cmdk :visible?] not)))

(rf/reg-event-db
 :cmdk/set
 (fn [db [_ visible?]]
   (assoc-in db [:cmdk :visible?] visible?)))
