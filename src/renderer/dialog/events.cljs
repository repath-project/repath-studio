(ns renderer.dialog.events
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.dialog.handlers :as h]
   [renderer.dialog.views :as v]))

(rf/reg-event-db
 ::cmdk
 (fn [db [_]]
   (h/create db {:content [v/cmdk]
                 :attrs {:class "dialog-content dialog-cmdk-content"}})))

(rf/reg-event-db
 ::about
 (fn [db [_]]
   (h/create db {:title config/app-name
                 :content [v/about]})))

(rf/reg-event-db
 ::confirmation
 (fn [db [_ data]]
   (h/create db {:title (:title data)
                 :content [v/confirmation data]})))

(rf/reg-event-db
 ::close
 (fn [db [_]]
   (update db :dialogs pop)))
