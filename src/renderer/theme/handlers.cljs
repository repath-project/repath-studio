(ns renderer.theme.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.theme.db :refer [ThemeMode]]))

(m/=> set-mode [:-> App ThemeMode App])
(defn set-mode
  [db mode]
  (assoc-in db [:theme :mode] mode))
