(ns renderer.app.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App Feature Platform]]))

(m/=> add-fx [:-> App vector? App])
(defn add-fx
  [db effect]
  (update db :fx conj effect))

(m/=> feature? [:-> App Feature boolean?])
(defn feature?
  [db k]
  (contains? (:features db) k))

(m/=> desktop? [:-> App Platform boolean?])
(defn desktop?
  [platform]
  (contains? #{"darwin" "win32" "linux"} platform))

(m/=> mobile? [:-> App Platform boolean?])
(defn mobile?
  [platform]
  (contains? #{"android" "ios"} platform))
