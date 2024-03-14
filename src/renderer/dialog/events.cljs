(ns renderer.dialog.events
  (:require
   [re-frame.core :as rf]
   [renderer.dialog.about :as about]
   [renderer.dialog.confirmation :as confirmation]
   [renderer.dialog.cmdk :as cmdk]
   [renderer.dialog.save :as save]))

(rf/reg-event-db
 :dialog/cmdk
 (fn [db [_]]
   (assoc db :dialog {:content [cmdk/root]})))

(rf/reg-event-db
 :dialog/about
 (fn [db [_]]
   (assoc db :dialog {:content [about/root]})))

(rf/reg-event-db
 :dialog/save
 (fn [db [_ k]]
   (assoc db :dialog {:content [save/root k]})))

(rf/reg-event-db
 :dialog/confirmation
 (fn [db [_ data]]
   (assoc db :dialog {:content [confirmation/root data]})))

(rf/reg-event-db
 :dialog/close
 (fn [db [_]]
   (dissoc db :dialog)))
