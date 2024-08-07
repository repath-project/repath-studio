(ns renderer.theme.events
  (:require
   [re-frame.core :as rf]
   [renderer.effects]
   [renderer.theme.effects :as fx]
   [renderer.utils.local-storage :as local-storage]))

(rf/reg-event-fx
 ::init-mode
 (fn [{:keys [db]} _]
   (let [mode (-> db :theme :mode)
         mode (if (= mode :system) (-> db :theme :native) mode)]
     {::fx/set-document-attr [(name mode)]})))

(rf/reg-event-fx
 ::set-native-mode
 local-storage/persist
 (fn [{:keys [db]} [_ mode]]
   {:db (assoc-in db [:theme :native] mode)
    :dispatch [::init-mode]}))

(rf/reg-event-fx
 ::set-mode
 local-storage/persist
 (fn [{:keys [db]} [_ mode]]
   {:db (assoc-in db [:theme :mode] mode)
    :dispatch [::init-mode]}))

(rf/reg-event-fx
 ::cycle-mode
 (fn [{:keys [db]} [_]]
   (let [mode (case (-> db :theme :mode)
                :dark :light
                :light :system
                :system :dark)]
     {:dispatch [::set-mode mode]})))
