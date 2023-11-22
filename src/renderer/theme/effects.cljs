(ns renderer.theme.effects
  (:require
   [platform]
   [re-frame.core :as rf]
   [renderer.utils.local-storage :as local-storage]))

(rf/reg-fx
 ::set-html-attribute
 (fn [[attr val]]
   (js/window.document.documentElement.setAttribute attr val)))

(rf/reg-event-fx
 :theme/init-mode
 (fn [{:keys [db]} _]
   (let [mode (-> db :theme :mode name)]
     {::set-html-attribute ["data-theme" mode]
      :send-to-main {:action "setThemeMode" :data mode}})))

(rf/reg-event-fx
 :theme/set-mode
 local-storage/persist
 (fn [{:keys [db]} [_ mode]]
   {:db (assoc-in db [:theme :mode] mode)
    :dispatch [:theme/init-mode]}))

(rf/reg-event-fx
 :theme/cycle-mode
 (fn [{:keys [db]} [_]]
   (let [mode (case (-> db :theme :mode)
                ;; TODO: system mode
                :dark :light
                :light :dark)]
     {:dispatch [:theme/set-mode mode]})))
