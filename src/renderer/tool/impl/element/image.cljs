(ns renderer.tool.impl.element.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.notification.events :as-alias notification.e]
   [renderer.tool.handlers :as tool.h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.drop :as drop]))

(derive :image ::hierarchy/element)

(defmethod hierarchy/properties :image
  []
  {:icon "image"})

(defmethod hierarchy/on-pointer-up :image
  [db]
  (tool.h/add-fx db [::app.fx/file-open
                     {:options {:startIn "pictures"
                                :types [{:accept {"image/png" [".png"]
                                                  "image/jpeg" [".jpeg" ".jpg"]
                                                  "image/bmp" [".fmp"]}}]}
                      :on-success [::success]
                      :on-error [::notification.e/exception]}]))

(rf/reg-event-fx
 ::success
 (fn [{:keys [db]} [_ file]]
   {:db (tool.h/activate db :transform)
    ::add-image [file (or (:point (:nearest-neighbor db))
                          (:adjusted-pointer-pos db))]}))

(rf/reg-fx
 ::add-image
 (fn [[file position]]
   (drop/add-image! file position)))
