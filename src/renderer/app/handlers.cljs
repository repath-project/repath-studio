(ns renderer.app.handlers
  (:require
   [renderer.tool.hierarchy :as tool.hierarchy]))

(defn set-state
  [db state]
  (assoc db :state state))

(defn set-cursor
  [db cursor]
  (assoc db :cursor cursor))

(defn set-message
  [db message]
  (assoc db :message message))

(defn explain
  [db message & rest]
  (assoc db :explanation (apply str message rest)))

(defn add-fx
  [db effect]
  (update db :fx conj effect))

(defn set-tool
  [db tool]
  (-> db
      (tool.hierarchy/deactivate)
      (assoc :tool tool)
      (tool.hierarchy/activate)))

