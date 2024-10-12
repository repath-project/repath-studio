(ns renderer.tool.misc.measure
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.app.handlers :as app.h]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.math :as math]
   [renderer.utils.units :as units]))

(derive :measure ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :measure
  []
  {:icon "ruler-triangle"})

(defmethod tool.hierarchy/help [:measure :default]
  []
  "Click and drag to measure a distance.")

(defmethod tool.hierarchy/activate :measure
  [db]
  (app.h/set-cursor db "crosshair"))

(defmethod tool.hierarchy/pointer-up :measure
  [db]
  (element.h/dissoc-temp db))

(defmethod tool.hierarchy/drag-end :measure  [db] db)

(defmethod tool.hierarchy/drag :measure
  [db]
  (let [{:keys [adjusted-pointer-offset adjusted-pointer-pos]} db
        [offset-x offset-y] adjusted-pointer-offset
        [x y] adjusted-pointer-pos
        [adjacent opposite] (mat/sub adjusted-pointer-offset adjusted-pointer-pos)
        hypotenuse (Math/hypot adjacent opposite)
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 x
               :y2 y
               :stroke "gray"}]
    (element.h/assoc-temp db {:id :mesure
                              :type :overlay
                              :tag :measure
                              :attrs attrs
                              :hypotenuse hypotenuse})))

(defmethod tool.hierarchy/render :measure
  [{:keys [attrs id hypotenuse]}]
  (let [{:keys [x1 x2 y1 y2]} attrs
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
      (str (.toFixed hypotenuse 2))
      [(/ (+ x1 x2) 2) (/ (+ y1 y2) 2)]]]))
