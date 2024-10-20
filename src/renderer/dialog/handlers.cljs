(ns renderer.dialog.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.dialog.db :refer [Dialog]]))

(m/=> create [:-> App Dialog App])
(defn create
  [db dialog]
  (update db :dialogs conj dialog))
