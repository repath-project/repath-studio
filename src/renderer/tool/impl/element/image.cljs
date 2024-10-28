(ns renderer.tool.impl.element.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [re-frame.core :as rf]
   [renderer.tool.events :as-alias tool.e]
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
                           (rf/dispatch [::tool.e/activate :transform])
                           (drop/add-image! file (or (:point (:nearest-neighbor db))
                                                     (:adjusted-pointer-pos db))))}) db)
