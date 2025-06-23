(ns renderer.notification.events
  (:require
   [re-frame.core :as rf]
   [renderer.notification.handlers :as notification.handlers]
   [renderer.notification.views :as notification.views]
   [renderer.utils.vec :as utils.vec]))

(rf/reg-event-db
 ::add
 (fn [db [_ notification]]
   (cond-> db notification (notification.handlers/add notification))))

(rf/reg-event-db
 ::show-unavailable-feature
 (fn [db [_ feature compatibility-url]]
   (notification.handlers/add db (notification.views/unavailable-feature
                                  feature
                                  compatibility-url))))

(rf/reg-event-db
 ::show-exception
 (fn [db [_ ^js/Error error]]
   (notification.handlers/add db (notification.views/exception error))))

(rf/reg-event-db
 ::remove-nth
 (fn [db [_ i]]
   (update db :notifications utils.vec/remove-nth i)))

(rf/reg-event-db
 ::clear-all
 (fn [db [_]]
   (assoc db :notifications [])))
