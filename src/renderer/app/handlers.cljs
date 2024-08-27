(ns renderer.app.handlers
  (:require
   [renderer.tool.base :as tool]))

(defn set-state
  [db state]
  (assoc db :state state))

(defn set-cursor
  [db cursor]
  (assoc db :cursor cursor))

(defn set-message
  [db message]
  (assoc db :message message))

(defn add-fx
  [db effect]
  (update db :fx conj effect))

(defn set-tool
  [db tool]
  (-> db
      (tool/deactivate)
      (assoc :tool tool)
      (tool/activate)))

