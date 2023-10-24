(ns renderer.window.effects
  (:require
   [re-frame.core :as rf]))

(rf/reg-fx
 :set-html-attribute
 (fn [[attr val]]
   (js/window.document.documentElement.setAttribute attr val)))

(rf/reg-fx
 ::close
 (fn [_]
   (.close js/window)))

(rf/reg-event-fx
 :window/cycle-theme-mode
 (fn [{:keys [db]} [_]]
   (let [theme (case (-> db :window :theme-mode)
                 ; TODO system mode
                 :dark :light
                 :light :dark)]
     {:db (assoc-in db [:window :theme-mode] theme)
      :set-html-attribute ["data-theme" (name theme)]
      :send-to-main {:action "setThemeMode" :data (name theme)}})))

(rf/reg-event-fx
 :window/close
 (fn [_ _]
   {::close nil}))

(rf/reg-event-fx
 :window/toggle-maximized
 (fn [_ _]
   {:send-to-main {:action "windowToggleMaximized"}}))

(rf/reg-event-fx
 :window/toggle-fullscreen
 (fn [_ _]
   {:send-to-main {:action "windowToggleFullscreen"}}))

(rf/reg-event-fx
 :window/minimize
 (fn [_ _]
   {:send-to-main {:action "windowMinimize"}}))

(rf/reg-event-fx
 :window/open-remote-url
 (fn [_ [_ url]]
   {:send-to-main {:action "openRemoteUrl" :data url}}))