(ns renderer.panel.views
  (:require
   ["react-resizable-panels" :refer [PanelResizeHandle]]))

(defn resize-handle
  [id]
  [:> PanelResizeHandle
   {:id id
    :class "resize-handle"}])
