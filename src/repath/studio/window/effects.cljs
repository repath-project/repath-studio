(ns repath.studio.window.effects
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :window/close
 (fn [_ _]
    {::close nil}))

(rf/reg-event-fx
 :window/toggle-maximized
 (fn [_ _]
   {:send-to-main {:action "windowToggleMaximized"}}))

(rf/reg-event-fx
 :window/minimize
 (fn [_ _]
   {:send-to-main {:action "windowMinimize"}}))

(rf/reg-event-fx
 :window/open-remote-url
 (fn [_ [_ url]]
    {:send-to-main {:action "openRemoteUrl" :data url}}))

(rf/reg-fx
 ::close
 (fn [_]
   (.close js/window)))