(ns renderer.window.events
  (:require
   [platform]
   [re-frame.core :as rf]))

(rf/reg-event-db
 :window/set-maximized?
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :maximized? state)))

(rf/reg-event-db
 :window/set-fullscreen?
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :fullscreen? state)))

(rf/reg-event-db
 :window/set-minimized?
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :minimized? state)))

(rf/reg-event-db
 :window/resize
 (rf/path :window)
 (fn [db [_ size]]
   (assoc db :size size)))

(rf/reg-fx
 ::close
 (fn [_]
   (.close js/window)))

(rf/reg-fx
 ::toggle-fullscreen
 (fn [_]
   (if (.-fullscreenElement js/document)
     (.exitFullscreen js/document)
     (.. js/document -documentElement requestFullscreen))))

(rf/reg-fx
 ::open-remote-url
 (fn [url]
   (.open js/window url)))

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
   (if platform/electron?
     {:send-to-main {:action "windowToggleFullscreen"}}
     {::toggle-fullscreen nil})))

(rf/reg-event-fx
 :window/minimize
 (fn [_ _]
   {:send-to-main {:action "windowMinimize"}}))

(rf/reg-event-fx
 :window/open-remote-url
 (fn [_ [_ url]]
   (if platform/electron?
     {:send-to-main {:action "openRemoteUrl" :data url}}
     {::open-remote-url url})))
