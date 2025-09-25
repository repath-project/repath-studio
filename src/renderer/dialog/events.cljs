(ns renderer.dialog.events
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.dialog.handlers :as dialog.handlers]
   [renderer.dialog.views :as dialog.views]))

(rf/reg-event-db
 ::show-cmdk
 (fn [db [_]]
   (dialog.handlers/create db {:title [:div.sr-only "Command panel"]
                               :content [dialog.views/cmdk]
                               :attrs {:class "top-10 translate-y-0 w-150"}})))

(rf/reg-event-db
 ::show-about
 (fn [db [_]]
   (dialog.handlers/create db {:title config/app-name
                               :has-close-button true
                               :content [dialog.views/about]})))

(rf/reg-event-db
 ::show-confirmation
 (fn [db [_ data]]
   (dialog.handlers/create db {:title (:title data)
                               :has-close-button true
                               :content [dialog.views/confirmation data]})))

(rf/reg-event-fx
 ::close
 (fn [{:keys [db]} [_ on-close]]
   (cond-> {:db (update db :dialogs pop)}
     on-close
     (assoc :dispatch on-close))))
