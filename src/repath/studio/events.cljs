(ns repath.studio.events
  (:require
   [re-frame.core :as rf]
   [repath.studio.tools.base :as tools]
   [repath.studio.db :refer [default-db]]))

(rf/reg-event-db
 :initialize-db
 (fn  [_ _]
   default-db))

(defn reg-set-event
  [k]
  (rf/reg-event-db
   (keyword (str "set-" (name k)))
   (fn [db [_ v]]
     (assoc-in db [k] v))))

(defn reg-toggle-event
  [k]
  (rf/reg-event-db
   (keyword (str "toggle-" (name k)))
   (fn [db [_]]
     (update db (keyword (str (name k) "?")) not))))

(doseq [x [:active-theme
           :state
           :cursor
           :left-sidebar-width
           :right-sidebar-width
           :active-document
           :mouse-tail
           :system-fonts
           :command-palette?
           :mouse-over-canvas?]] (reg-set-event x))

(rf/reg-event-db
 :set-tool
 (fn [db [_ tool]]
   (-> db
    (tools/deactivate)   
    (assoc :tool tool)
    (tools/activate))))

(doseq [x [:tree
           :properties
           :header
           :history
           :debug-info
           :elements-collapsed
           :pages-collapsed
           :symbols-collapsed
           :defs-collapsed]] (reg-toggle-event x))