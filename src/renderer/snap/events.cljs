(ns renderer.snap.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.snap.handlers :as h]))

(rf/reg-event-db
 ::toggle
 [persist]
 (fn [db [_]]
   (update-in db [:snap :enabled?] not)))

(rf/reg-event-db
 ::toggle-option
 [persist]
 (fn [db [_ option]]
   (h/toggle-option db option)))
