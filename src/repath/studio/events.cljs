(ns repath.studio.events
  (:require
   [re-frame.core :as rf]
   [repath.studio.tools.base :as tools]
   [repath.studio.db :refer [default-db]]))

(rf/reg-event-db
 :initialize-db
 (fn  [_ _]
   default-db))

(rf/reg-event-db
 :set-cursor
 (fn [db [_ type]]
   (assoc db :cursor type)))

(rf/reg-event-db
 :set-active-document
 (fn [db [_ document-id]]
   (assoc db :active-document document-id)))

(rf/reg-event-db
 :set-system-fonts
 (fn [db [_ fonts]]
   (assoc db :system-fonts fonts)))

(rf/reg-event-db
 :set-command-palette?
 (fn [db [_ visible?]]
   (assoc db :command-palette? visible?)))

(rf/reg-event-db
 :set-tool
 (fn [db [_ tool]]
   (tools/set-tool db tool)))

(rf/reg-event-db
 :toggle-debug-info
 (fn [db [_]]
   (update db :debug-info? not)))