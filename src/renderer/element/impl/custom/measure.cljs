(ns renderer.element.impl.custom.measure
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.utils.math :as math]
   [renderer.utils.overlay :as overlay]
   [renderer.utils.units :as units]))

(derive :measure ::hierarchy/element)

(defmethod hierarchy/render :measure
  [el]
  (let [{:keys [attrs id]} el
        {:keys [x1 x2 y1 y2 hypotenuse]} attrs
        [x1 y1 x2 y2] (map units/unit->px [x1 y1 x2 y2])
        angle (math/angle [x1 y1] [x2 y2])
        zoom @(rf/subscribe [::document.s/zoom])
        straight? (< angle 180)
        straight-angle (if straight? angle (- angle 360))]
    [:g {:key id}
     [overlay/cross x1 y1]
     [overlay/cross x2 y2]

     [overlay/arc [x1 y1] 20 (if straight? 0 angle) (abs straight-angle)]

     [overlay/line x1 y1 x2 y2 false]
     [overlay/line x1 y1 (+ x1 (/ 30 zoom)) y1]

     [overlay/label
      (str (.toFixed straight-angle 2) "°")
      [(+ x1 (/ 40 zoom)) y1]
      "start"]

     [overlay/label
      (-> hypotenuse js/parseFloat (.toFixed 2) str)
      [(/ (+ x1 x2) 2) (/ (+ y1 y2) 2)]]]))
