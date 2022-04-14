(ns repath.studio.window.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-db
 :window/set-bitmap-data
 (rf/path :window)
 (fn [db [_ data]]
   (assoc db 
          :bitmap (.-bitmap data)
          :size (js->clj (.-size data) :keywordize-keys true))))

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
 :window/toggle-tree
 (rf/path :window)
 (fn [db [_]]
   (update db :tree? not)))

(rf/reg-event-db
 :window/toggle-properties
 (rf/path :window)
 (fn [db [_]]
   (update db :properties? not)))

(rf/reg-event-db
 :window/toggle-header
 (rf/path :window)
 (fn [db [_]]
   (update db :header? not)))

(rf/reg-event-db
 :window/toggle-xml
 (rf/path :window)
 (fn [db [_]]
   (update db :xml? not)))

(rf/reg-event-db
 :window/toggle-elements-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :elements-collapsed? not)))

(rf/reg-event-db
 :window/toggle-pages-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :pages-collapsed? not)))

(rf/reg-event-db
 :window/toggle-symbols-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :symbols-collapsed? not)))

(rf/reg-event-db
 :window/toggle-repl-history-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :repl-history-collapsed? not)))

(rf/reg-event-db
 :window/toggle-defs-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :defs-collapsed? not)))