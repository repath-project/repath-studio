(ns renderer.theme.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.theme.db :refer [ThemeMode NativeMode]]))

(m/=> set-mode [:-> App ThemeMode App])
(defn set-mode
  [db mode]
  (assoc-in db [:theme :mode] mode))

(m/=> set-native-mode [:-> App NativeMode App])
(defn set-native-mode
  [db native-mode]
  (assoc-in db [:theme :native-mode] native-mode))
