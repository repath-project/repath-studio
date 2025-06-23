(ns renderer.tool.impl.element.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [re-frame.core :as rf]
   [renderer.effects :as-alias effects]
   [renderer.element.effects :as-alias element.fx]
   [renderer.notification.events :as-alias notification.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :image ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :image
  []
  {:icon "image"})

(defmethod tool.hierarchy/on-drag-end :image
  [db e]
  (tool.hierarchy/on-pointer-up db e))

(defmethod tool.hierarchy/on-pointer-up :image
  [db _e]
  (tool.handlers/add-fx db [::effects/file-open
                            {:options {:startIn "pictures"
                                       :types [{:accept {"image/png" [".png"]
                                                         "image/jpeg" [".jpeg" ".jpg"]
                                                         "image/bmp" [".bmp"]
                                                         "image/gif" [".gif"]}}]}
                             :on-success [::success]
                             :on-error [::notification.events/show-exception]}]))

(rf/reg-event-fx
 ::success
 (fn [{:keys [db]} [_ file]]
   {:db (tool.handlers/activate db :transform)
    ::element.fx/import-image [file (or (:point (:nearest-neighbor db))
                                        (:adjusted-pointer-pos db))]}))
