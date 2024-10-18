(ns renderer.tool.impl.element.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.drop :as drop]
   [renderer.utils.file :as file]))

(derive :image ::hierarchy/element)

(defmethod hierarchy/properties :image
  []
  {:icon "image"})

(defmethod hierarchy/pointer-up :image
  [db]
  (file/open! {:options {:startIn "pictures"
                         :types [{:accept {"image/png" [".png"]
                                           "image/jpeg" [".jpeg" ".jpg"]
                                           "image/bmp" [".fmp"]}}]}
               :callback (fn [file]
                           (rf/dispatch [::app.e/set-tool :transform])
                           (drop/add-image! file (:adjusted-pointer-pos db)))}) db)
