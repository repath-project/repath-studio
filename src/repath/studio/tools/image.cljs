(ns repath.studio.tools.image
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]))

(derive :image ::tools/graphics)

(defmethod tools/properties :image [] {:icon "image"
                                       :description "The <image> SVG element includes images inside SVG documents. It can display raster image files or other SVG files."})

(defmethod tools/drag :image
  [{:keys [adjusted-mouse-pos tool adjusted-mouse-offset fill stroke]}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (Math/abs (- pos-x offset-x))
               :height (Math/abs (- pos-y offset-y))}]
    (rf/dispatch [:set-temp-element {:type tool
                                     :attrs attrs}])))

(defmethod tools/bounds :image
  [_ {{:keys [x y width height]} :attrs}]
  [x y (+ x width) (+ y height)])
