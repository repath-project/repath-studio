(ns repath.studio.window.effects
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :window/close
 (fn [_ _]
   (.close js/window)))

(rf/reg-event-fx
 :window/toggle-maximized
 (fn [_ _]
   (js/window.api.send "toMain" #js {:action "windowToggleMaximized"})))

(rf/reg-event-fx
 :window/minimize
 (fn [_ _]
   (js/window.api.send "toMain" #js {:action "windowMinimize"})))

(rf/reg-event-fx
 :window/open-remote-url
 (fn [_ [_ url]]
   (js/window.api.send "toMain" #js {:action "openRemoteUrl" :data url})))