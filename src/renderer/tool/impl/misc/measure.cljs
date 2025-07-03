(ns renderer.tool.impl.misc.measure
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.handlers :as element.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]
   [renderer.utils.math :as utils.math]
   [renderer.utils.svg :as utils.svg]))

(derive :measure ::tool.hierarchy/tool)

(defonce attrs (reagent/atom nil))

(rf/reg-fx
 ::update
 (fn [value]
   (reset! attrs value)))

(defmethod tool.hierarchy/properties :measure
  []
  {:icon "ruler-triangle"})

(defmethod tool.hierarchy/help [:measure :idle]
  []
  (t [::help "Click and drag to measure a distance."]))

(defmethod tool.hierarchy/on-activate :measure
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defmethod tool.hierarchy/on-deactivate :measure
  [db]
  (tool.handlers/add-fx db [::update nil]))

(defmethod tool.hierarchy/on-drag :measure
  [db _e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        [adjacent opposite] (matrix/sub [offset-x offset-y] [x y])
        hypotenuse (Math/hypot adjacent opposite)]
    (tool.handlers/add-fx db [::update {:x1 offset-x
                                        :y1 offset-y
                                        :x2 x
                                        :y2 y
                                        :hypotenuse hypotenuse
                                        :stroke "gray"}])))

(defmethod tool.hierarchy/render :measure
  []
  (when @attrs
    (let [{:keys [x1 x2 y1 y2 hypotenuse]} @attrs
          [x1 y1 x2 y2] (map utils.length/unit->px [x1 y1 x2 y2])
          angle (utils.math/angle [x1 y1] [x2 y2])
          zoom @(rf/subscribe [::document.subs/zoom])
          straight? (< angle 180)
          straight-angle (if straight? angle (- angle 360))]
      [:g
       [utils.svg/cross [x1 y1]]
       [utils.svg/cross [x2 y2]]

       [utils.svg/arc [x1 y1] 20 (if straight? 0 angle) (abs straight-angle)]

       [utils.svg/line [x1 y1] [x2 y2] false]
       [utils.svg/line [x1 y1] [(+ x1 (/ 30 zoom)) y1]]

       [utils.svg/label
        (str (utils.length/->fixed straight-angle 2 false) "°")
        [(+ x1 (/ 40 zoom)) y1]
        "start"]

       [utils.svg/label
        (-> hypotenuse js/parseFloat (utils.length/->fixed 2 false) str)
        [(/ (+ x1 x2) 2) (/ (+ y1 y2) 2)]]])))

(defmethod tool.hierarchy/snapping-points :measure
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (str "measure " (if @attrs "end" "start"))})])

(defmethod tool.hierarchy/snapping-elements :measure
  [db]
  (filter :visible (vals (element.handlers/entities db))))
