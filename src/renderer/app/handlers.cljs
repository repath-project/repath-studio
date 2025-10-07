(ns renderer.app.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]))

(m/=> add-fx [:-> App vector? App])
(defn add-fx
  [db effect]
  (update db :fx conj effect))

(defn feature?
  [db k]
  (contains? (:features db) k))
