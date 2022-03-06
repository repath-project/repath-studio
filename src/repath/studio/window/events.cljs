(ns repath.studio.window.events
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

(rf/reg-event-db
 :window/set-bitmap
 (fn [db [_ bitmap]]
   (assoc db 
          :window/bitmap (.-bitmap bitmap)
          :window/bitmap-size (js->clj (.-size bitmap) :keywordize-keys true))))

(rf/reg-event-db
 :window/set-maximized?
 (fn [db [_ state]]
  (assoc db :window/maximized? state)))

(rf/reg-event-db
 :window/set-fullscreen?
 (fn [db [_ state]]
   (assoc db :window/fullscreen? state)))

(rf/reg-event-db
 :window/set-minimized?
 (fn [db [_ state]]
   (assoc db :window/minimized? state)))