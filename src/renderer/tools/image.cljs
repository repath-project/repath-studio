(ns renderer.tools.image
  "https://www.w3.org/TR/SVG/embedded.html#ImageElement"
  (:require
   [renderer.element.handlers :as elements]
   [renderer.tools.base :as tools]))

(derive :image ::tools/graphics)
(derive :image ::tools/box)

(defmethod tools/properties :image
  []
  {:icon "image"
   :description "The <image> SVG element includes images inside SVG documents. 
                 It can display raster image files or other SVG files."
   :attrs [:href]})

(defmethod tools/drag :image
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos] :as db}]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :width (abs (- pos-x offset-x))
               :height (abs (- pos-y offset-y))
               :preserveAspectRatio "xMidYMid slice"}]
    (elements/set-temp db {:type :element :tag :image :attrs attrs})))
