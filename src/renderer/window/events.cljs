(ns renderer.window.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.document.handlers :as document.handlers]
   [renderer.event.events :as-alias event.events]
   [renderer.event.impl.keyboard :as event.keyboard]
   [renderer.utils.system :as utils.system]
   [renderer.window.effects :as window.effects]))

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
     {::window.effects/ipc-send ["relaunch"]}
     {::window.effects/relaunch nil})))

(rf/reg-event-fx
 ::clear-local-storage-and-relaunch
 (fn [_ _]
   {:fx [[::app.effects/local-storage-clear nil]
         [:dispatch [::relaunch]]]}))

(rf/reg-event-fx
 ::toggle-maximized
 (fn [_ _]
   {::window.effects/ipc-send ["window-toggle-maximized"]}))

(rf/reg-event-fx
 ::toggle-fullscreen
 (fn [_ _]
   (if utils.system/electron?
     {::window.effects/ipc-send ["window-toggle-fullscreen"]}
     {::window.effects/toggle-fullscreen nil})))

(rf/reg-event-fx
 ::minimize
 (fn [_ _]
   {::window.effects/ipc-send ["window-minimize"]}))

(rf/reg-event-fx
 ::open-remote-url
 (fn [_ [_ url]]
   (if utils.system/electron?
     {::window.effects/ipc-send ["open-remote-url" url]}
     {::window.effects/open-remote-url url})))

(rf/reg-event-fx
 ::add-listeners
 (fn [_ _]
   {:fx [[::window.effects/add-document-event-listener ["keydown" [::event.events/keyboard] event.keyboard/->clj]]
         [::window.effects/add-document-event-listener ["keyup" [::event.events/keyboard] event.keyboard/->clj]]
         [::window.effects/add-document-event-listener ["fullscreenchange" [::update-fullscreen]]]
         [::window.effects/add-event-listener ["load" [::update-focused]]]
         [::window.effects/add-event-listener ["focus" [::update-focused]]]
         [::window.effects/add-event-listener ["blur" [::update-focused]]]]}))

(rf/reg-event-fx
 ::register-listeners
 (fn [_ _]
   (if utils.system/electron?
     {:fx [[::window.effects/ipc-on ["window-maximized" [::set-maximized true]]]
           [::window.effects/ipc-on ["window-unmaximized" [::set-maximized false]]]
           [::window.effects/ipc-on ["window-focused" [::set-focused true]]]
           [::window.effects/ipc-on ["window-blurred" [::set-focused false]]]
           [::window.effects/ipc-on ["window-entered-fullscreen" [::set-fullscreen true]]]
           [::window.effects/ipc-on ["window-leaved-fullscreen" [::set-fullscreen false]]]
           [::window.effects/ipc-on ["window-minimized" [::set-minimized true]]]
           [::window.effects/ipc-on ["window-loaded" [::add-listeners]]]]}
     {:dispatch [::add-listeners]})))
