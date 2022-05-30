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
   {::toggle-maximized nil}))

(rf/reg-event-fx
 :window/minimize
 (fn [_ _]
   {::minimize nil}))

(rf/reg-event-fx
 :window/open-remote-url
 (fn [_ [_ url]]
   {::open-remote-url url}))

(rf/reg-fx
 ::close
 (fn [_]
   (.close js/window)))

(rf/reg-fx
 ::toggle-maximized
 (fn [_]
   (js/window.api.send "toMain" #js {:action "windowToggleMaximized"})))

(rf/reg-fx
 ::minimize
 (fn [_]
   (js/window.api.send "toMain" #js {:action "windowMinimize"})))

(rf/reg-fx
 ::open-remote-url
 (fn [url]
   (js/window.api.send "toMain" #js {:action "openRemoteUrl" :data url})))