(ns renderer.tool.shape.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.data-transfer :as data-transfer]
   [renderer.utils.file :as file]))

(derive :image ::tool.hierarchy/graphics)
(derive :image ::tool.hierarchy/box)

(defmethod tool.hierarchy/properties :image
  []
  {:icon "image"
   :description "The <image> SVG element includes images inside SVG documents.
                 It can display raster image files or other SVG files."
   :attrs [:href]})

(defmethod tool.hierarchy/pointer-up :image
  [{:keys [adjusted-pointer-pos] :as db}]
  (file/open!
   {:startIn "pictures"
    :types [{:accept {"image/png" [".png"]
                      "image/jpeg" [".jpeg" ".jpg"]
                      "image/bmp" [".fmp"]}}]}
   (fn [file]
     (rf/dispatch [::app.e/set-tool :select])
     (data-transfer/add-image! file adjusted-pointer-pos)))
  db)
