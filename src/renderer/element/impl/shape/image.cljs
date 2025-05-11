(ns renderer.element.impl.shape.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/image"
  (:require
   [renderer.element.hierarchy :as element.hierarchy]))

(derive :image ::element.hierarchy/graphics)
(derive :image ::element.hierarchy/box)

(defmethod element.hierarchy/properties :image
  []
  {:icon "image"
   :description "The <image> SVG element includes images inside SVG documents.
                 It can display raster image files or other SVG files."
   :attrs [:href]})
