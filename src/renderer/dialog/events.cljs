(ns renderer.dialog.events
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.dialog.handlers :as dialog.handlers]
   [renderer.dialog.views :as dialog.views]))

(rf/reg-event-db
 ::cmdk
 (fn [db [_]]
   (dialog.handlers/create db {:title [:div.sr-only "Command panel"]
                               :content (dialog.views/cmdk)
                               :attrs {:class "dialog-content"
                                       :style {:top "33px"
                                               :transform "translate(-50%, 0)"}}})))

(rf/reg-event-db
 ::about
 (fn [db [_]]
   (dialog.handlers/create db {:title config/app-name
                               :close-button true
                               :content (dialog.views/about)})))

(rf/reg-event-db
 ::confirmation
 (fn [db [_ data]]
   (dialog.handlers/create db {:title (:title data)
                               :close-button true
                               :content (dialog.views/confirmation data)})))

(rf/reg-event-fx
 ::close
 (fn [{:keys [db]} [_ event]]
   (cond-> {:db (update db :dialogs pop)}
     event
     (assoc :dispatch event))))
