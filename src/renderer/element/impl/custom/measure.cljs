(ns renderer.element.impl.custom.measure
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.utils.length :as length]
   [renderer.utils.math :as math]
   [renderer.utils.svg :as svg]))

(derive :measure ::hierarchy/element)

(defmethod hierarchy/render :measure
  [el]
  (let [{:keys [attrs id]} el
        {:keys [x1 x2 y1 y2 hypotenuse]} attrs
        [x1 y1 x2 y2] (map length/unit->px [x1 y1 x2 y2])
        angle (math/angle [x1 y1] [x2 y2])
        zoom @(rf/subscribe [::document.s/zoom])
        straight? (< angle 180)
        straight-angle (if straight? angle (- angle 360))]
    [:g {:key id}
     [svg/cross [x1 y1]]
     [svg/cross [x2 y2]]

     [svg/arc [x1 y1] 20 (if straight? 0 angle) (abs straight-angle)]

     [svg/line [x1 y1] [x2 y2] false]
     [svg/line [x1 y1] [(+ x1 (/ 30 zoom)) y1]]

     [svg/label
      (str (.toFixed straight-angle 2) "°")
      [(+ x1 (/ 40 zoom)) y1]
      "start"]

     [svg/label
      (-> hypotenuse js/parseFloat (.toFixed 2) str)
      [(/ (+ x1 x2) 2) (/ (+ y1 y2) 2)]]]))
