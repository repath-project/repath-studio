(ns renderer.element.impl.shape.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [renderer.app.events :as-alias app.e]
   [renderer.element.hierarchy :as hierarchy]))

(derive :image ::hierarchy/graphics)
(derive :image ::hierarchy/box)

(defmethod hierarchy/properties :image
  []
  {:icon "image"
   :description "The <image> SVG element includes images inside SVG documents.
                 It can display raster image files or other SVG files."
   :attrs [:href]})
