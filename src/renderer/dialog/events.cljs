(ns renderer.dialog.events
  (:require
   [re-frame.core :as rf]
   [renderer.dialog.views :as v]
   [renderer.dialog.cmdk :as cmdk]))

(rf/reg-event-db
 ::cmdk
 (fn [db [_]]
   (assoc db :dialog {:content [cmdk/root]
                      :attrs {:class "dialog-content dialog-cmdk-content"}})))

(rf/reg-event-db
 ::about
 (fn [db [_]]
   (assoc db :dialog {:content [v/about]})))

(rf/reg-event-db
 ::save
 (fn [db [_ k]]
   (assoc db :dialog {:content [v/save k]
                      :attrs {:onOpenAutoFocus #(.preventDefault %)}})))

#_(rf/reg-event-db
   ::confirmation
   (fn [db [_ data]]
     (assoc db :dialog {:content [v/confirmation data]})))

(rf/reg-event-db
 ::close
 (fn [db [_]]
   (dissoc db :dialog)))
