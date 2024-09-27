(ns renderer.theme.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :refer [persist]]
   [renderer.theme.effects :as-alias fx]))

(rf/reg-event-fx
 ::add-native-listener
 (fn [_ _]
   {::fx/add-native-listener ::set-native-mode}))

(rf/reg-event-fx
 ::set-document-attr
 (fn [{:keys [db]} _]
   (let [mode (-> db :theme :mode)
         mode (if (= mode :system) (-> db :theme :native-mode) mode)]
     {::fx/set-document-attr (name mode)})))

(rf/reg-event-fx
 ::set-native-mode
 persist
 (fn [{:keys [db]} [_ mode]]
   {:db (assoc-in db [:theme :native-mode] mode)
    :dispatch [::set-document-attr]}))

(rf/reg-event-fx
 ::cycle-mode
 persist
 (fn [{:keys [db]} [_]]
   (let [mode (case (-> db :theme :mode)
                :dark :light
                :light :system
                :system :dark)]
     {:db (assoc-in db [:theme :mode] mode)
      :dispatch [::set-document-attr]})))
