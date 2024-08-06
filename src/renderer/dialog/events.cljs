(ns renderer.dialog.events
  (:require
   [re-frame.core :as rf]
   [renderer.dialog.views :as v]))

(defn create
  [db dialog]
  (update db :dialogs conj dialog))

(rf/reg-event-db
 ::cmdk
 (fn [db [_]]
   (create db {:content [v/cmdk]
               :attrs {:class "dialog-content dialog-cmdk-content"}})))

(rf/reg-event-db
 ::about
 (fn [db [_]]
   (create db {:title "Repath Studio"
               :content [v/about]})))

(rf/reg-event-fx
 ::save
 (fn [{:keys [db]} [_ k]]
   {:db (create db {:title "Do you want to save your changes?"
                    :content [v/save k]
                    :attrs {:onOpenAutoFocus #(.preventDefault %)}})}))

(rf/reg-event-db
 ::confirmation
 (fn [db [_ data]]
   (create db {:title (:title data)
               :content [v/confirmation data]})))

(rf/reg-event-db
 ::close
 (fn [db [_]]
   (update db :dialogs pop)))
