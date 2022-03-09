(ns repath.studio.tools.image
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]))

(derive :image ::tools/graphics)

(defmethod tools/properties :image [] {:icon "image"
                                       :description "The <image> SVG element includes images inside SVG documents. It can display raster image files or other SVG files."
                                       :attrs [:href
                                               :xlink:href]})

(defmethod tools/drag :image
  [{:keys [adjusted-mouse-offset] :as db} _ _ {:keys [adjusted-mouse-pos fill stroke]}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (Math/abs (- pos-x offset-x))
               :height (Math/abs (- pos-y offset-y))
               :preserveAspectRatio "xMidYMid slice"}]
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :image :attrs attrs}))))