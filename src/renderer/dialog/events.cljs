(ns renderer.dialog.events
  (:require
   [re-frame.core :as rf]
   [renderer.dialog.views :as v]
   [renderer.dialog.cmdk :as cmdk]))

(rf/reg-event-db
 :dialog/cmdk
 (fn [db [_]]
   (assoc db :dialog {:content [cmdk/root]
                      :attrs {:class "dialog-content dialog-cmdk-content"}})))

(rf/reg-event-db
 :dialog/about
 (fn [db [_]]
   (assoc db :dialog {:content [v/about]})))

(rf/reg-event-db
 :dialog/save
 (fn [db [_ k]]
   (assoc db :dialog {:content [v/save k]})))

(rf/reg-event-db
 :dialog/shortcuts
 (fn [db [_ k]]
   (assoc db :dialog {:content [v/shortcuts]})))

(rf/reg-event-db
 :dialog/confirmation
 (fn [db [_ data]]
   (assoc db :dialog {:content [v/confirmation data]})))

(rf/reg-event-db
 :dialog/close
 (fn [db [_]]
   (dissoc db :dialog)))
