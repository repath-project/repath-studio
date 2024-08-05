(ns renderer.window.events
  (:require
   [platform]
   [re-frame.core :as rf]))

(rf/reg-event-db
 ::set-maximized?
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :maximized? state)))

(rf/reg-event-db
 ::set-fullscreen?
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :fullscreen? state)))

(rf/reg-event-db
 ::set-minimized?
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :minimized? state)))

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
 ::close
 (fn [_ _]
   {::close nil}))

(rf/reg-event-fx
 ::toggle-maximized
 (fn [_ _]
   {:ipc-send ["window-toggle-maximized"]}))

(rf/reg-event-fx
 ::toggle-fullscreen
 (fn [_ _]
   (if platform/electron?
     {:ipc-send ["window-toggle-fullscreen"]}
     {::toggle-fullscreen nil})))

(rf/reg-event-fx
 ::minimize
 (fn [_ _]
   {:ipc-send ["window-minimize"]}))

(rf/reg-event-fx
 ::open-remote-url
 (fn [_ [_ url]]
   (if platform/electron?
     {:ipc-send ["open-remote-url" url]}
     {::open-remote-url url})))
