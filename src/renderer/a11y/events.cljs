(ns renderer.a11y.events
  (:require
   [re-frame.core :as rf]
   [renderer.a11y.handlers :as handlers]))

(rf/reg-event-db
 ::register-filter
 (fn [db [_ a11y-filter]]
   (handlers/register-filter db a11y-filter)))

(rf/reg-event-db
 ::deregister-filter
 (fn [db [_ a11y-filter-id]]
   (handlers/deregister-filter db a11y-filter-id)))
