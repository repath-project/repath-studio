(ns renderer.tool.misc.measure
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as handlers]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.math :as math]
   [renderer.utils.units :as units]))

(derive :measure ::tool/tool)

(defmethod tool/properties :measure
  []
  {:icon "ruler-triangle"})

(defmethod tool/activate :measure
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (handlers/set-message  [:div "Click and drag to measure a distance."])))

(defmethod tool/deactivate :measure
  [db]
  (element.h/clear-temp db))

(defmethod tool/pointer-up :measure
  [db]
  (element.h/clear-temp db))

(defmethod tool/drag-end :measure  [db] db)

(defmethod tool/drag :measure
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos] :as db}]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        [adjacent opposite] (mat/sub adjusted-pointer-offset
                                     adjusted-pointer-pos)
        hypotenuse (Math/hypot adjacent opposite)
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 pos-x
               :y2 pos-y
               :stroke "gray"}]
    (element.h/set-temp db {:type :overlay
                            :tag :measure
                            :attrs attrs
                            :hypotenuse hypotenuse})))

(defmethod tool/render :measure
  [{:keys [attrs key hypotenuse]}]
  (let [{:keys [x1 x2 y1 y2]} attrs
        [x1 y1 x2 y2] (map units/unit->px [x1 y1 x2 y2])
        angle (math/angle x1 y1 x2 y2)
        zoom @(rf/subscribe [::document.s/zoom])
        straight? (< angle 180)
        straight-angle (if straight? angle (- angle 360))]
    [:g {:key key}
     [overlay/cross x1 y1]
     [overlay/cross x2 y2]

     [overlay/arc [x1 y1] 20 (if straight? 0 angle) (abs straight-angle)]

     [overlay/line x1 y1 x2 y2 false]
     [overlay/line x1 y1 (+ x1 (/ 30 zoom)) y1]

     [overlay/label
      (str (units/->fixed straight-angle) "°")
      [(+ x1 (/ 40 zoom)) y1]
      "start"]

     [overlay/label
      (str (units/->fixed hypotenuse))
      [(/ (+ x1 x2) 2) (/ (+ y1 y2) 2)]]]))
