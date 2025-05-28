(ns renderer.window.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as-alias effects]
   [renderer.utils.system :as utils.system]
   [renderer.window.effects :as-alias window.effects]))

(rf/reg-event-db
 ::set-maximized
 (fn [db [_ state]]
   (assoc-in db [:window :maximized] state)))

(rf/reg-event-db
 ::set-fullscreen
 (fn [db [_ state]]
   (assoc-in db [:window :fullscreen] state)))

(rf/reg-event-db
 ::set-minimized
 (fn [db [_ state]]
   (assoc-in db [:window :minimized] state)))

(rf/reg-event-db
 ::set-focused
 (fn [db [_ state]]
   (cond-> db
     :always (assoc-in [:window :focused] state)
     state document.handlers/center)))

(rf/reg-event-fx
 ::update-focused
 [(rf/inject-cofx ::window.effects/focused)]
 (fn [{:keys [db focused]} _]
   {:db (cond-> (assoc-in db [:window :focused] focused)
          focused
          document.handlers/center)}))

(rf/reg-event-fx
 ::update-fullscreen
 [(rf/inject-cofx ::window.effects/fullscreen)]
 (fn [{:keys [db fullscreen]} _]
   {:db (assoc-in db [:window :fullscreen] fullscreen)}))

(rf/reg-event-fx
 ::close
 (fn [_ _]
   {::window.effects/close nil}))

(rf/reg-event-fx
 ::relaunch
 (fn [_ _]
   (if utils.system/electron?
     {::effects/ipc-send ["relaunch"]}
     {::window.effects/reload nil})))

(rf/reg-event-fx
 ::clear-local-storage-and-relaunch
 (fn [_ _]
   {:fx [[::app.effects/local-storage-clear nil]
         [:dispatch [::relaunch]]]}))

(rf/reg-event-fx
 ::toggle-maximized
 (fn [_ _]
   {::effects/ipc-send ["window-toggle-maximized"]}))

(rf/reg-event-fx
 ::toggle-fullscreen
 (fn [_ _]
   (if utils.system/electron?
     {::effects/ipc-send ["window-toggle-fullscreen"]}
     {::window.effects/toggle-fullscreen nil})))

(rf/reg-event-fx
 ::minimize
 (fn [_ _]
   {::effects/ipc-send ["window-minimize"]}))
