(ns renderer.panel.events
  (:require
   [re-frame.core :as rf]
   [renderer.utils.local-storage :as local-storage]))

(rf/reg-event-db
 :panel/toggle
 [local-storage/persist
  (rf/path :panel)]
 (fn [db [_ key]]
   (update-in db [key :visible?] not)))

(rf/reg-event-db
 :panel/collapse
 [local-storage/persist
  (rf/path :panel)]
 (fn [db [_ key]]
   (assoc-in db [key :visible?] false)))

(rf/reg-event-db
 :panel/expand
 [local-storage/persist
  (rf/path :panel)]
 (fn [db [_ key]]
   (assoc-in db [key :visible?] true)))
