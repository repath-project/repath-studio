(ns repath.studio.tools.rect
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]))

(derive :rect ::tools/shape)

(defmethod tools/properties :rect [] {:icon "rectangle"
                                      :description "The <rect> element is a basic SVG shape that draws rectangles, defined by their position, width, and height. The rectangles may have their corners rounded."
                                      :attrs [:stroke-width
                                              :opacity
                                              :fill
                                              :stroke]})

(defmethod tools/drag :rect
  [{:keys [adjusted-mouse-offset] :as db} _ _ {:keys [adjusted-mouse-pos fill stroke]}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (Math/abs (- pos-x offset-x))
               :height (Math/abs (- pos-y offset-y))
               :fill   (tools/rgba fill)
               :stroke (tools/rgba stroke)}]
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :rect :attrs attrs}))))
