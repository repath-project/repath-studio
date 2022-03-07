(ns repath.studio.events
  (:require
   [re-frame.core :as rf]
   [repath.studio.handlers :as handlers]
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
           :overlay
           :system-fonts
           :command-palette?
           :mouse-over-canvas?]] (reg-set-event x))

(rf/reg-event-db
 :set-tool
 (fn [db [_ tool]]
   (handlers/set-tool db tool)))

(doseq [x [:tree
           :properties
           :header
           :history
           :debug-info
           :elements-collapsed
           :pages-collapsed
           :symbols-collapsed
           :repl-history-collapsed
           :defs-collapsed]] (reg-toggle-event x))