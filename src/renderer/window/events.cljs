(ns renderer.window.events
  (:require
   [platform :as platform]
   [re-frame.core :as rf]
   [renderer.document.handlers :as document.h]
   [renderer.window.effects :as fx]))

(rf/reg-event-db
 ::set-maximized
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :maximized? state)))

(rf/reg-event-db
 ::set-fullscreen
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :fullscreen? state)))

(rf/reg-event-db
 ::set-minimized
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :minimized? state)))

(rf/reg-event-db
 ::set-focused
 (fn [db [_ focused?]]
   (-> db
       (assoc-in [:window :focused?] focused?)
       document.h/center)))

(rf/reg-event-fx
 ::close
 (fn [_ _]
   {::fx/close nil}))

(rf/reg-event-fx
 ::toggle-maximized
 (fn [_ _]
   {:ipc-send ["window-toggle-maximized"]}))

(rf/reg-event-fx
 ::toggle-fullscreen
 (fn [_ _]
   (if platform/electron?
     {:ipc-send ["window-toggle-fullscreen"]}
     {::fx/toggle-fullscreen nil})))

(rf/reg-event-fx
 ::minimize
 (fn [_ _]
   {:ipc-send ["window-minimize"]}))

(rf/reg-event-fx
 ::open-remote-url
 (fn [_ [_ url]]
   (if platform/electron?
     {:ipc-send ["open-remote-url" url]}
     {::fx/open-remote-url url})))
