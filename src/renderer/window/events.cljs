(ns renderer.window.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as app.fx]
   [renderer.document.handlers :as document.h]
   [renderer.utils.system :as system]
   [renderer.window.effects :as fx]))

(rf/reg-event-db
 ::set-maximized
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :maximized state)))

(rf/reg-event-db
 ::set-fullscreen
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :fullscreen state)))

(rf/reg-event-db
 ::set-minimized
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :minimized state)))

(rf/reg-event-db
 ::set-focused
 (fn [db [_ state]]
   (cond-> db
     :always (assoc-in [:window :focused] state)
     state document.h/center)))

(rf/reg-event-fx
 ::close
 (fn [_ _]
   {::fx/close nil}))

(rf/reg-event-fx
 ::relaunch
 (fn [_ _]
   (if system/electron?
     {::fx/ipc-send ["relaunch"]}
     {::fx/relaunch nil})))

(rf/reg-event-fx
 ::clear-local-storage-and-relaunch
 (fn [_ _]
   {:fx [[::app.fx/local-storage-clear nil]
         [:dispatch [::relaunch]]]}))

(rf/reg-event-fx
 ::toggle-maximized
 (fn [_ _]
   {::fx/ipc-send ["window-toggle-maximized"]}))

(rf/reg-event-fx
 ::toggle-fullscreen
 (fn [_ _]
   (if system/electron?
     {::fx/ipc-send ["window-toggle-fullscreen"]}
     {::fx/toggle-fullscreen nil})))

(rf/reg-event-fx
 ::minimize
 (fn [_ _]
   {::fx/ipc-send ["window-minimize"]}))

(rf/reg-event-fx
 ::open-remote-url
 (fn [_ [_ url]]
   (if system/electron?
     {::fx/ipc-send ["open-remote-url" url]}
     {::fx/open-remote-url url})))
