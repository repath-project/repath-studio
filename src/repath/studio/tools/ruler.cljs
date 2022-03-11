(ns repath.studio.tools.ruler
  (:require [re-frame.core :as rf]
            [repath.studio.elements.handlers :as elements]
            [clojure.core.matrix :as matrix]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [repath.studio.styles :as styles]
            [goog.string :as gstring]))

(derive :ruler ::tools/edit)

(defmethod tools/properties :ruler [] {:icon "ruler-triangle"})

(defmethod tools/activate :ruler
  [db] 
  (assoc db :cursor "crosshair"))

(defmethod tools/deactivate :ruler
  [db]
  (elements/clear-temp db))

(defmethod tools/mouse-up :ruler [db] db)

(defmethod tools/drag-end :ruler [db] db)

(defmethod tools/drag :ruler
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        [adjacent opposite] (matrix/sub adjusted-mouse-offset adjusted-mouse-pos)
        hypotenuse (Math/sqrt (+ (Math/pow adjacent 2) (Math/pow opposite 2)))
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 pos-x
               :y2 pos-y
               :stroke styles/accent}]
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :ruler :attrs attrs :hypotenuse hypotenuse}))))

(defmethod tools/render :ruler
  [{:keys [attrs key hypotenuse]}]
  (let [{:keys [x1 x2 y1 y2]} attrs
        [x1 y1 x2 y2] (map units/unit->px [x1 y1 x2 y2])
        zoom @(rf/subscribe [:zoom])
        cross-size (/ 5 zoom)]
    [:g {:key key}
     [:line {:x1 (- x1 cross-size) :y1 y1 :x2 (+ x1 cross-size) :y2 y1 :stroke-width (/ 1 zoom) :stroke styles/accent}]
     [:line {:x1 x1 :y1 (- y1 cross-size) :x2 x1 :y2 (+ y1 cross-size) :stroke-width (/ 1 zoom) :stroke styles/accent}]
     [:line {:x1 (- x2 cross-size) :y1 y2 :x2 (+ x2 cross-size) :y2 y2 :stroke-width (/ 1 zoom) :stroke styles/accent}]
     [:line {:x1 x2 :y1 (- y2 cross-size) :x2 x2 :y2 (+ y2 cross-size) :stroke-width (/ 1 zoom) :stroke styles/accent}]
     [:text {:x (+ x1 (/ 10 zoom)) :y (- y1 (/ 10 zoom)) :font-size (/ 12 zoom) :fill "black"} (gstring/format "%.4f" hypotenuse)]
     [:line (merge attrs {:stroke-width (/ 1 zoom)})]]))
