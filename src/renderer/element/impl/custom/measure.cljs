(ns renderer.element.impl.custom.measure
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.utils.length :as utils.length]
   [renderer.utils.math :as utils.math]
   [renderer.utils.svg :as utils.svg]))

(derive :measure ::element.hierarchy/element)

(defmethod element.hierarchy/render :measure
  [el]
  (let [{:keys [attrs id]} el
        {:keys [x1 x2 y1 y2 hypotenuse]} attrs
        [x1 y1 x2 y2] (map utils.length/unit->px [x1 y1 x2 y2])
        angle (utils.math/angle [x1 y1] [x2 y2])
        zoom @(rf/subscribe [::document.subs/zoom])
        straight? (< angle 180)
        straight-angle (if straight? angle (- angle 360))]
    [:g {:key id}
     [utils.svg/cross [x1 y1]]
     [utils.svg/cross [x2 y2]]

     [utils.svg/arc [x1 y1] 20 (if straight? 0 angle) (abs straight-angle)]

     [utils.svg/line [x1 y1] [x2 y2] false]
     [utils.svg/line [x1 y1] [(+ x1 (/ 30 zoom)) y1]]

     [utils.svg/label
      (str (.toFixed straight-angle 2) "°")
      [(+ x1 (/ 40 zoom)) y1]
      "start"]

     [utils.svg/label
      (-> hypotenuse js/parseFloat (.toFixed 2) str)
      [(/ (+ x1 x2) 2) (/ (+ y1 y2) 2)]]]))
