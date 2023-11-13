(ns renderer.window.events
  (:require
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
 :window/toggle-header
 (rf/path :window)
 (fn [db [_]]
   (update db :header? not)))