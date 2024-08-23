(ns renderer.handlers
  (:require
   [renderer.db :as db]
   [renderer.tool.base :as tool]
   [renderer.utils.spec :as spec]))

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
      tool/deactivate
      (assoc :tool tool)
      tool/activate))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec"
  [db event]
  (when (not (db/valid? db))
    (js/console.error (str "Event: " (first event)))
    (throw (js/Error. (str "Spec check failed: " (spec/explain db db/app))))))
