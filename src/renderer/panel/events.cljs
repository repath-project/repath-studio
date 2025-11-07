(ns renderer.panel.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.panel.handlers :as panel.handlers]))

(rf/reg-event-db
 ::toggle
 [persist]
 (fn [db [_ k]]
   (panel.handlers/toggle db k)))
