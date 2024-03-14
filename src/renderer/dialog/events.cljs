(ns renderer.dialog.events
  (:require
   [re-frame.core :as rf]
   [renderer.dialog.about :as about]
   [renderer.dialog.cmdk :as cmdk]))

(rf/reg-event-db
 :dialog/cmdk
 (fn [db [_]]
   (assoc db :dialog {:content [cmdk/root]})))

(rf/reg-event-db
 :dialog/about
 (fn [db [_]]
   (assoc db :dialog {:content [about/root]})))

(rf/reg-event-db
 :dialog/close
 (fn [db [_]]
   (dissoc db :dialog)))
