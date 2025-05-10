(ns renderer.window.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.document.events :as-alias document.e]
   [renderer.document.handlers :as document.h]
   [renderer.tool.events :as-alias tool.e]
   [renderer.utils.keyboard :as keyboard]
   [renderer.utils.system :as system]
   [renderer.window.effects :as fx]))

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
     state document.h/center)))

(rf/reg-event-fx
 ::update-focused
 [(rf/inject-cofx ::fx/focused)]
 (fn [{:keys [db focused]} _]
   {:db (cond-> (assoc-in db [:window :focused] focused)
          focused
          document.h/center)}))

(rf/reg-event-fx
 ::update-fullscreen
 [(rf/inject-cofx ::fx/fullscreen)]
 (fn [{:keys [db fullscreen]} _]
   {:db (assoc-in db [:window :focused] fullscreen)}))

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

(rf/reg-event-fx
 ::add-listeners
 (fn [_ _]
   {:fx [[::fx/add-document-event-listener ["keydown" [::tool.e/keyboard-event] keyboard/event-formatter]]
         [::fx/add-document-event-listener ["keyup" [::tool.e/keyboard-event] keyboard/event-formatter]]
         [::fx/add-document-event-listener ["fullscreenchange" [::update-fullscreen]]]
         [::fx/add-event-listener ["load" [::update-focused]]]
         [::fx/add-event-listener ["focus" [::update-focused]]]
         [::fx/add-event-listener ["blur" [::update-focused]]]
         [:dispatch [::document.e/center]]]}))

(rf/reg-event-fx
 ::register-listeners
 (fn [_ _]
   (if system/electron?
     {:fx [[::fx/ipc-on ["window-maximized" [::set-maximized true]]]
           [::fx/ipc-on ["window-unmaximized" [::set-maximized false]]]
           [::fx/ipc-on ["window-focused" [::set-focused true]]]
           [::fx/ipc-on ["window-blurred" [::set-focused false]]]
           [::fx/ipc-on ["window-entered-fullscreen" [::set-fullscreen true]]]
           [::fx/ipc-on ["window-leaved-fullscreen" [::set-fullscreen false]]]
           [::fx/ipc-on ["window-minimized" [::set-minimized true]]]
           [::fx/ipc-on ["window-loaded" [::add-listeners]]]]}
     {:dispatch [::add-listeners]})))
