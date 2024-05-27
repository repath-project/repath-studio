(ns renderer.notification.events
  (:require
   [re-frame.core :as rf]
   [renderer.notification.handlers :as h]
   [renderer.notification.views :as v]
   [renderer.utils.vec :as vec]))

(rf/reg-event-db
 :notification/add
 (fn [db [_ notification]]
   (h/add db notification)))

(rf/reg-event-db
 :notification/unavailable-feature
 (fn [db [_ feature compatibility-url]]
   (h/add db (v/unavailable-feature feature compatibility-url))))

(rf/reg-event-db
 :notification/remove
 (fn [db [_ i]]
   (update db :notifications vec/remove-nth i)))

(rf/reg-event-db
 :notification/clear-all
 (fn [db [_]]
   (assoc db :notifications [])))
