(ns renderer.app.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App Feature Platform]]))

(m/=> add-fx [:-> App vector? App])
(defn add-fx
  [db effect]
  (update db :fx conj effect))

(m/=> supported-feature? [:-> App Feature boolean?])
(defn supported-feature?
  [db k]
  (contains? (:features db) k))

(m/=> desktop? [:-> Platform boolean?])
(defn desktop?
  [platform]
  (contains? #{"darwin" "win32" "linux"} platform))

(m/=> mobile? [:-> Platform boolean?])
(defn mobile?
  [platform]
  (contains? #{"android" "ios"} platform))

(m/=> web? [:-> Platform boolean?])
(defn web?
  [platform]
  (= platform "web"))
