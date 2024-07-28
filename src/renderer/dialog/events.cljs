(ns renderer.dialog.events
  (:require
   [re-frame.core :as rf]
   [renderer.dialog.views :as v]
   [renderer.dialog.cmdk :as cmdk]))

(rf/reg-event-db
 ::cmdk
 (fn [db [_]]
   (update db :dialogs conj {:content [cmdk/root]
                             :attrs {:class "dialog-content dialog-cmdk-content"}})))

(rf/reg-event-db
 ::about
 (fn [db [_]]
   (update db :dialogs conj {:content [v/about]})))

(rf/reg-event-db
 ::save
 (fn [db [_ k]]
   (update db :dialogs conj {:content [v/save k]
                             :attrs {:onOpenAutoFocus #(.preventDefault %)}})))

#_(rf/reg-event-db
   ::confirmation
   (fn [db [_ data]]
     (update db :dialogs conj {:content [v/confirmation data]})))

(rf/reg-event-db
 ::close
 (fn [db [_]]
   (update db :dialogs pop)))
