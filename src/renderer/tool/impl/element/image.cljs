(ns renderer.tool.impl.element.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [re-frame.core :as rf]
   [renderer.effects :as-alias effects]
   [renderer.element.db :as element.db]
   [renderer.element.effects :as-alias element.effects]
   [renderer.notification.events :as-alias notification.events]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.i18n :refer [t]]))

(derive :image ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :image
  []
  {:icon "image"
   :label (t [::label "Image"])})

(defmethod tool.hierarchy/on-drag-end :image
  [db e]
  (tool.hierarchy/on-pointer-up db e))

(defmethod tool.hierarchy/on-pointer-up :image
  [db _e]
  (tool.handlers/add-fx db [::effects/file-open
                            {:options {:startIn "pictures"
                                       :types [{:accept element.db/image-mime-types}]}
                             :on-success [::success]
                             :on-error [::notification.events/show-exception]}]))

(rf/reg-event-fx
 ::success
 (fn [{:keys [db]} [_ file]]
   {:db (tool.handlers/activate db :transform)
    ::element.effects/import-image {:file file
                                    :on-success [::element.effects/add]
                                    :position (or (:point (:nearest-neighbor db))
                                                  (:adjusted-pointer-pos db))}}))
