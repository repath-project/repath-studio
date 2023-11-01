(ns renderer.theme.effects
  (:require
   [re-frame.core :as rf]
   [platform]))

(rf/reg-fx
 ::set-html-attribute
 (fn [[attr val]]
   (js/window.document.documentElement.setAttribute attr val)))

(rf/reg-event-fx
 :theme/cycle
 (fn [{:keys [db]} [_]]
   (let [theme (case (-> db :theme-mode)
                 ; TODO system mode
                 :dark :light
                 :light :dark)]
     {:db (assoc-in db [:theme-mode] theme)
      ::set-html-attribute ["data-theme" (name theme)]
      :send-to-main {:action "setThemeMode" :data (name theme)}})))