(ns renderer.panel.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.panel.db :refer [PanelId]]))

(m/=> toggle [:-> App PanelId App])
(defn toggle
  [db id]
  (update-in db [:panels id :visible] not))
